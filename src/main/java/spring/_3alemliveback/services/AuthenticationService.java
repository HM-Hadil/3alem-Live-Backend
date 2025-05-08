package spring._3alemliveback.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring._3alemliveback.dto.register.*;
import spring._3alemliveback.entities.Token;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.Role;
import spring._3alemliveback.exceptions.EmailAlreadyExistsException;
import spring._3alemliveback.exceptions.UserNotFoundException;
import spring._3alemliveback.mapper.UserMapper;
import spring._3alemliveback.repo.TokenRepository;
import spring._3alemliveback.repo.UserRepository;
import spring._3alemliveback.security.JwtService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    @Autowired
    private UserMapper userMapper;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // Add a method to find user for authentication without loading LOB data
    private Optional<User> findUserForAuthentication(String email) {
        // Create a custom query or repository method that fetches user without LOB fields
        // For demonstration, we'll use findByEmail but you should create a custom query
        return userRepository.findByEmail(email);
    }


    public AuthenticationResponse registerApprenant(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        // Créer un nouvel utilisateur avec les champs spécifiques à l'apprenant
        var user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.USER)
               // .domaines(request.getDomaines()) // Liste des domaines pour l'apprenant
                .isActive(true)
                .isVerified(false)
                .verificationToken(java.util.UUID.randomUUID().toString()) // Generate token
                .build();

        var savedUser = userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());

        saveUserToken(savedUser, jwtToken);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthenticationResponse registerExpert(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        // Vérifier que les champs obligatoires pour l'expert sont bien fournis
        if (request.getCertifications() == null || request.getCertifications().isEmpty()) {
            throw new IllegalArgumentException("Les certifications sont requises pour les experts.");
        }

        if (request.getProfileDescription() == null || request.getProfileDescription().isEmpty()) {
            throw new IllegalArgumentException("La description du profil est requise pour les experts.");
        }

        // Créer un nouvel utilisateur avec les champs spécifiques à l'expert
        var user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.EXPERT)
                .certifications(request.getCertifications())  // Certificats PDF
                .profileDescription(request.getProfileDescription())
                .experience(request.getExperience())
                .linkedinUrl(request.getLinkedinUrl())
                .niveauEtude(request.getNiveauEtude())
                .portfolioUrl(request.getPortfolioUrl())
                .cvPdf(request.getCvPdf())
                .profileImage(request.getProfileImage())
                .domaines(request.getDomaines())
                .isActive(false) // L'admin devra valider l'expert manuellement
                .isVerified(false)
                .verificationToken(java.util.UUID.randomUUID().toString()) // Générer un token
                .build();

        var savedUser = userRepository.save(user);
        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);

        // Envoi du mail de vérification
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getVerificationToken());

        saveUserToken(savedUser, jwtToken);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // First authenticate with the provided credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // After authentication succeeds, fetch minimal user information needed for token generation
        // Avoid loading LOB fields by using a projection or specific query
        User userForAuth = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Only access the essential fields needed for JWT token generation
        var jwtToken = jwtService.generateToken(userForAuth);
        var refreshToken = jwtService.generateRefreshToken(userForAuth);

        revokeAllUserTokens(userForAuth);
        saveUserToken(userForAuth, jwtToken);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void validateExpertAccount(Long expertId) {
        User user = userRepository.findById(expertId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != Role.EXPERT) {
            throw new IllegalStateException("L'utilisateur n'est pas un expert");
        }

        if (!user.isVerified()) {
            throw new IllegalStateException("L'utilisateur n'a pas encore vérifié son compte par email");
        }

        user.setActive(true); // C'est maintenant que l'admin active l'expert
        userRepository.save(user);
    }

    /**
     * Récupérer la liste des experts qui ont vérifié leur email mais n'ont pas encore été activés par l'admin
     * @return Liste des experts en attente de validation
     */
    public List<UserDto> getPendingExpertDtos() {
        List<User> pendingExperts = userRepository.findByRoleAndIsVerifiedTrueAndIsActiveFalse(Role.EXPERT);
        return userMapper.toDtoList(pendingExperts);
    }

    public List<User> getPendingExperts() {
        return userRepository.findByRoleAndIsVerifiedTrueAndIsActiveFalse(Role.EXPERT);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void verifyAccount(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token invalide ou expiré"));

        user.setVerified(true);
        user.setVerificationToken(null); // Supprime le token après vérification
        // Activer directement si c'est un USER
        if (user.getRole() == Role.USER) {
            user.setActive(true);
        }
        userRepository.save(user);
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;

        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    @Transactional
    public User updateUserProfile(String userEmail, UserProfileUpdateRequest updateRequest) {
        // Find the user by email from the authenticated principal
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Update user fields from the update request DTO
        if (updateRequest.getNom() != null) user.setNom(updateRequest.getNom());
        if (updateRequest.getPrenom() != null) user.setPrenom(updateRequest.getPrenom());
        if (updateRequest.getPhone() != null) user.setPhone(updateRequest.getPhone());
        if (updateRequest.getProfileDescription() != null) user.setProfileDescription(updateRequest.getProfileDescription());
        if (updateRequest.getNiveauEtude() != null) user.setNiveauEtude(updateRequest.getNiveauEtude());
        if (updateRequest.getExperience() != null) user.setExperience(updateRequest.getExperience());
        if (updateRequest.getLinkedinUrl() != null) user.setLinkedinUrl(updateRequest.getLinkedinUrl());
        if (updateRequest.getPortfolioUrl() != null) user.setPortfolioUrl(updateRequest.getPortfolioUrl());

        // Handle profile image update safely
        if (updateRequest.getProfileImage() != null) {
            if (!updateRequest.getProfileImage().isEmpty()) {
                try {
                    byte[] decodedImage = java.util.Base64.getDecoder().decode(updateRequest.getProfileImage());
                    user.setProfileImage(decodedImage);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid Base64 string for profile image", e);
                }
            } else {
                user.setProfileImage(null);
            }
        }

        // Handle CV update safely
        if (updateRequest.getCvPdf() != null) {
            if (!updateRequest.getCvPdf().isEmpty()) {
                try {
                    byte[] decodedCv = java.util.Base64.getDecoder().decode(updateRequest.getCvPdf());
                    user.setCvPdf(decodedCv);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid Base64 string for CV", e);
                }
            } else {
                user.setCvPdf(null);
            }
        }

        // Handle certifications update
        if (updateRequest.getCertifications() != null) {
            user.setCertifications(updateRequest.getCertifications());
        }

        // Update domains if provided
        if (updateRequest.getDomaines() != null) {
            user.setDomaines(updateRequest.getDomaines());
        }

        // Save the updated user entity
        return userRepository.save(user);
    }

    // Method to get user by email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}