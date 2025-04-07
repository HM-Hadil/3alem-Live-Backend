package spring._3alemliveback.services;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring._3alemliveback.dto.register.AuthenticationRequest;
import spring._3alemliveback.dto.register.AuthenticationResponse;
import spring._3alemliveback.dto.register.RegisterRequest;
import spring._3alemliveback.entities.Token;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.Role;
import spring._3alemliveback.exceptions.EmailAlreadyExistsException;
import spring._3alemliveback.exceptions.UserNotFoundException;
import spring._3alemliveback.repo.TokenRepository;
import spring._3alemliveback.repo.UserRepository;
import spring._3alemliveback.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;


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

                .domaines(request.getDomaines()) // Liste des domaines pour l'apprenant
                .isActive(true)
                .isVerified(false)
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

    public void validateExpertAccount(Long expertId) {
        User user = userRepository.findById(expertId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));

        if (user.getRole() != Role.EXPERT) {
            throw new IllegalStateException("L'utilisateur n'est pas un expert");
        }

        if (!user.isVerified()) {
            throw new IllegalStateException("L'utilisateur n'a pas encore vérifié son compte par email");
        }

        user.setActive(true); // C’est maintenant que l’admin active l’expert
        userRepository.save(user);
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

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                .build();
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
}