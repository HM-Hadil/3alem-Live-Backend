package spring._3alemliveback.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Importer Slf4j
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importer Transactional
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

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // Importer UUID

@Service
@RequiredArgsConstructor
@Slf4j // Ajouter l'annotation Slf4j
public class AuthenticationService {

    @Autowired // Conserver si vous préférez l'injection par champ, sinon utiliser le constructeur avec final
    private UserMapper userMapper;

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // Méthode d'enregistrement pour Apprenant
    @Transactional // Ajoutez Transactional
    public AuthenticationResponse registerApprenant(RegisterRequest request) {
        log.info("Tentative d'enregistrement d'un apprenant: {}", request.getEmail());
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Échec de l'enregistrement: Email déjà utilisé - {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email déjà utilisé");
        }

        // Créer un nouvel utilisateur avec les champs spécifiques à l'apprenant
        // Le token de vérification est généré ici et uniquement ici
        String verificationToken = UUID.randomUUID().toString();
        log.info("Token de vérification généré pour {}: {}", request.getEmail(), verificationToken);

        var user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.ADMIN)
                // .domaines(request.getDomaines()) // Assurez-vous que ce champ existe dans User si décommenté
                .isActive(true) // Apprenants actifs dès la vérification email (ou toujours?) - à confirmer selon votre logique métier
                .isVerified(false) // Non vérifié tant que l'email n'est pas confirmé
                .verificationToken(verificationToken) // Définir le token généré ici
                .build();

        var savedUser = userRepository.save(user);
        log.info("Apprenant enregistré avec succès, ID: {}", savedUser.getId());

        // Générer et sauvegarder les tokens JWT (si connexion automatique après inscription sans vérification immédiate)
        // Sinon, vous pouvez omettre cette partie et l'utilisateur devra se connecter après vérification email.
        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);
        saveUserToken(savedUser, jwtToken);
        log.info("Tokens JWT générés et sauvegardés pour {}", savedUser.getEmail());


        // Envoi du mail de vérification
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getVerificationToken());
            log.info("Email de vérification envoyé à: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de vérification à {}: {}", savedUser.getEmail(), e.getMessage(), e); // Logger l'exception complète
            // Décidez comment gérer l'erreur d'envoi d'email (retirer l'utilisateur? marquer comme en attente d'email?)
        }


        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                // Inclure l'état de vérification et d'activité dans la réponse
                .isVerified(savedUser.isVerified())
                .isActive(savedUser.isActive())
                .build();
    }

    // Méthode d'enregistrement pour Expert
    @Transactional // Ajoutez Transactional
    public AuthenticationResponse registerExpert(RegisterRequest request) {
        log.info("Tentative d'enregistrement d'un expert: {}", request.getEmail());
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Échec de l'enregistrement: Email déjà utilisé - {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email déjà utilisé");
        }

        // Vérifier que les champs obligatoires pour l'expert sont bien fournis
        // (La validation devrait idéalement se faire au niveau du DTO ou du Contrôleur avec @Valid)
        if (request.getCertifications() == null || request.getCertifications().isEmpty()) {
            log.warn("Échec de l'enregistrement: Certifications manquantes pour expert {}", request.getEmail());
            throw new IllegalArgumentException("Les certifications sont requises pour les experts.");
        }

        if (request.getProfileDescription() == null || request.getProfileDescription().isEmpty()) {
            log.warn("Échec de l'enregistrement: Description de profil manquante pour expert {}", request.getEmail());
            throw new IllegalArgumentException("La description du profil est requise pour les experts.");
        }

        // Le token de vérification est généré ici et uniquement ici
        String verificationToken = UUID.randomUUID().toString();
        log.info("Token de vérification généré pour {}: {}", request.getEmail(), verificationToken);

        // Créer un nouvel utilisateur avec les champs spécifiques à l'expert
        var user = User.builder()
                .nom(request.getNom())
                .prenom(request.getPrenom())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.EXPERT)
                .certifications(request.getCertifications())
                .profileDescription(request.getProfileDescription())
                .experience(request.getExperience())
                .linkedinUrl(request.getLinkedinUrl())
                .niveauEtude(request.getNiveauEtude())
                .portfolioUrl(request.getPortfolioUrl())
                .cvPdf(request.getCvPdf())
                .profileImage(request.getProfileImage())
                .domaines(request.getDomaines()) // Assurez-vous que ce champ existe dans User si décommenté
                .isActive(false) // L'expert est inactif jusqu'à validation admin
                .isVerified(false) // Non vérifié tant que l'email n'est pas confirmé
                .verificationToken(verificationToken) // Définir le token généré ici
                .build();
        log.debug("État de l'utilisateur AVANT sauvegarde: ID={}, Email={}, Token={}", user.getId(), user.getEmail(), user.getVerificationToken());


        var savedUser = userRepository.save(user);
        log.info("Expert enregistré avec succès, ID: {}", savedUser.getId());

        // Générer et sauvegarder les tokens JWT (si connexion automatique après inscription sans vérification immédiate)
        // Sinon, vous pouvez omettre cette partie et l'utilisateur devra se connecter après vérification email.
        var jwtToken = jwtService.generateToken(savedUser);
        var refreshToken = jwtService.generateRefreshToken(savedUser);
        saveUserToken(savedUser, jwtToken);
        log.info("Tokens JWT générés et sauvegardés pour {}", savedUser.getEmail());


        // Envoi du mail de vérification
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getVerificationToken());
            log.info("Email de vérification envoyé à: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de vérification à {}: {}", savedUser.getEmail(), e.getMessage(), e); // Logger l'exception complète
            // Décidez comment gérer l'erreur d'envoi d'email
        }


        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                // Inclure l'état de vérification et d'activité dans la réponse
                .isVerified(savedUser.isVerified())
                .isActive(savedUser.isActive())
                .build();
    }

    @Transactional // Ajoutez Transactional si ce n'est pas déjà fait
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Tentative d'authentification pour: {}", request.getEmail());
        // Spring Security vérifiera user.isEnabled() (qui est mappé à isVerified)
        // et user.isAccountNonLocked() (qui est mappé à isActive)
        // Si isVerified=false ou isActive=false, l'authentification échouera avec les exceptions standards de Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        log.info("Authentification réussie pour: {}", request.getEmail());

        // Après authentification réussie, récupérer l'utilisateur pour générer les tokens
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Erreur critique: Utilisateur authentifié non trouvé dans la base de données: {}", request.getEmail());
                    return new UserNotFoundException("Utilisateur introuvable");
                });


        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user); // Révoquer les anciens tokens JWT
        saveUserToken(user, jwtToken); // Sauvegarder le nouveau token JWT
        log.info("Tokens JWT générés et sauvegardés pour {}", user.getEmail());


        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken)
                // Inclure l'état de vérification et d'activité dans la réponse de connexion
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .build();
    }

    @Transactional // Ajoutez Transactional
    public void validateExpertAccount(Long expertId) {
        log.info("Tentative de validation admin pour l'expert ID: {}", expertId);
        User user = userRepository.findById(expertId)
                .orElseThrow(() -> {
                    log.warn("Échec validation admin: Utilisateur non trouvé ID: {}", expertId);
                    return new UserNotFoundException("Utilisateur non trouvé");
                });

        if (user.getRole() != Role.EXPERT) {
            log.warn("Échec validation admin: L'utilisateur ID {} n'est pas un expert (rôle: {})", expertId, user.getRole());
            throw new IllegalStateException("L'utilisateur n'est pas un expert");
        }

        if (!user.isVerified()) {
            log.warn("Échec validation admin: L'expert {} n'a pas encore vérifié son email.", user.getEmail());
            throw new IllegalStateException("L'utilisateur n'a pas encore vérifié son compte par email");
        }

        if (user.isActive()) {
            log.warn("Échec validation admin: L'expert {} est déjà actif.", user.getEmail());
            // Optionnel: lancer une exception si tenter de valider un compte déjà actif est une erreur
            // throw new IllegalStateException("Le compte expert est déjà actif");
            return; // ou simplement retourner si ce n'est pas une erreur bloquante
        }

        user.setActive(true); // C'est maintenant que l'admin active l'expert
        userRepository.save(user);
        log.info("Compte expert activé par l'admin: {}", user.getEmail());
    }

    /**
     * Récupérer la liste des experts qui ont vérifié leur email mais n'ont pas encore été activés par l'admin
     * @return Liste des experts en attente de validation
     */
    @Transactional(readOnly = true) // Lecture seule
    public List<UserDto> getPendingExpertDtos() {
        List<User> pendingExperts = userRepository.findByRoleAndIsVerifiedTrueAndIsActiveFalse(Role.EXPERT);
        log.info("Récupération de {} experts en attente de validation admin.", pendingExperts.size());
        return userMapper.toDtoList(pendingExperts);
    }
    @Transactional(readOnly = true) // Lecture seule
    public List<User> getPendingExperts() {
        log.info("Récupération des entités experts en attente de validation admin.");
        return userRepository.findByRoleAndIsVerifiedTrueAndIsActiveFalse(Role.EXPERT);
    }
    @Transactional(readOnly = true) // Lecture seule
    public List<User> getActivateExperts() {
        log.info("Récupération des entités experts en attente de validation admin.");
        return userRepository.findByRoleAndIsVerifiedTrueAndIsActiveTrue(Role.EXPERT);
    }
    /**
     * Récupérer la liste des experts actifs (qui ont été validés par l'admin)
     * @return Liste des experts actifs
     */
    @Transactional(readOnly = true) // Lecture seule
    public List<User> getAllExperts() {
        log.info("Récupération de tous les experts actifs");
        return userRepository.findByRoleAndIsVerifiedTrue(Role.EXPERT);
    }
    @Transactional(readOnly = true) // Lecture seule
    public Optional<User> getUserById(Long id) {
        log.debug("Recherche utilisateur par ID: {}", id);
        return userRepository.findById(id);
    }

    @Transactional // Ajoutez Transactional
    public void verifyAccount(String token) {
        log.info("Tentative de vérification avec token: {}", token); // Utiliser le logger

        Optional<User> userOpt = userRepository.findByVerificationToken(token);

        if (!userOpt.isPresent()) {
            log.warn("Aucun utilisateur trouvé avec ce token de vérification: {}", token); // Utiliser le logger

            // --- Débogage temporaire: Lister tous les tokens ---
            // COMMENTEZ OU SUPPRIMEZ ce bloc pour la production
            log.debug("--- Début du listing de TOUS les tokens de vérification pour débogage ---");
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getVerificationToken() != null) {
                    log.debug("User ID: {}, Email: {}, Token: {}", u.getId(), u.getEmail(), u.getVerificationToken());
                } else {
                    log.debug("User ID: {}, Email: {}, Token: null", u.getId(), u.getEmail());
                }
            }
            log.debug("--- Fin du listing de TOUS les tokens ---");
            // --- Fin du débogage temporaire ---


            // Lancer l'exception (qui sera attrapée par le contrôleur pour afficher l'erreur)
            throw new RuntimeException("Token invalide ou expiré"); // Conserver le message pour le front-end si le contrôleur l'affiche directement
        }

        User user = userOpt.get();
        log.info("Utilisateur trouvé pour vérification: {}", user.getEmail()); // Utiliser le logger

        if (user.isVerified()) {
            log.warn("Compte {} est déjà vérifié.", user.getEmail()); // Utiliser le logger
            throw new RuntimeException("Ce compte a déjà été vérifié"); // Message pour le front-end
        }


        user.setVerified(true); // Marquer comme vérifié par email
        user.setVerificationToken(null); // Invalider le token

        // Logique d'activation : les Users sont actifs après vérification email, les Experts attendent validation admin
        if (user.getRole() == Role.USER) {
            user.setActive(true);
            log.info("Compte utilisateur {} marqué comme actif.", user.getEmail());
        } else if (user.getRole() == Role.EXPERT) {
            log.info("Compte expert {} vérifié, en attente de validation admin pour l'activation.", user.getEmail());
            // isActive reste false
        }


        userRepository.save(user);
        log.info("Compte vérifié et mis à jour avec succès pour: {}", user.getEmail()); // Utiliser le logger
    }

    @Transactional // Ajoutez Transactional
    public void resendVerificationEmail(String email) {
        log.info("Demande de renvoi d'email de vérification pour: {}", email); // Utiliser le logger
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Échec renvoi email: Utilisateur non trouvé - {}", email);
                    return new UserNotFoundException("Utilisateur non trouvé");
                });

        if (user.isVerified()) {
            log.warn("Échec renvoi email: Le compte {} est déjà vérifié.", user.getEmail());
            throw new IllegalStateException("Ce compte a déjà été vérifié");
        }

        // Générer un nouveau token (même si un ancien existait, on le remplace)
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        userRepository.save(user); // Sauvegarder le nouveau token
        log.info("Nouveau token de vérification ({}) généré et sauvegardé pour {}.", newToken, email);


        // Envoyer le nouvel email
        try {
            emailService.sendVerificationEmail(user.getEmail(), newToken);
            log.info("Nouvel email de vérification envoyé à: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Échec de l'envoi du nouvel email de vérification à {}: {}", user.getEmail(), e.getMessage(), e); // Logger l'exception complète
            throw new RuntimeException("Erreur lors de l'envoi de l'email.", e);
        }
    }

    // --- Méthodes existantes (JWT, etc.) ---

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .expired(false) // Tokens JWT expirent par durée, non par ce flag
                .revoked(false) // Tokens JWT sont révoqués lors de la déconnexion ou nouvelle connexion
                .build();
        tokenRepository.save(token);
    }

    @Transactional // Ajoutez Transactional
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


    // Conservez refreshToken, updateUserProfile, getUserById, getUserByEmail tels quels ou avec ajout de logs/Transactional

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    @Transactional
    public User updateUserProfile(String userEmail, UserProfileUpdateRequest updateRequest) {
        log.info("Mise à jour du profil pour l'utilisateur: {}", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("Erreur critique: Utilisateur authentifié non trouvé pour mise à jour profil: {}", userEmail);
                    return new UserNotFoundException("User not found");
                });

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
                    log.debug("Image de profil décodée et définie.");
                } catch (IllegalArgumentException e) {
                    log.error("Erreur de décodage Base64 pour l'image de profil.", e);
                    throw new RuntimeException("Chaîne Base64 invalide pour l'image de profil", e);
                }
            } else {
                user.setProfileImage(null);
                log.debug("Image de profil définie à null.");
            }
        }

        // Handle CV update safely
        if (updateRequest.getCvPdf() != null) {
            if (!updateRequest.getCvPdf().isEmpty()) {
                try {
                    byte[] decodedCv = java.util.Base64.getDecoder().decode(updateRequest.getCvPdf());
                    user.setCvPdf(decodedCv);
                    log.debug("CV décodé et défini.");
                } catch (IllegalArgumentException e) {
                    log.error("Erreur de décodage Base64 pour le CV.", e);
                    throw new RuntimeException("Chaîne Base64 invalide pour le CV", e);
                }
            } else {
                user.setCvPdf(null);
                log.debug("CV défini à null.");
            }
        }

        // Handle certifications update
        if (updateRequest.getCertifications() != null) {
            user.setCertifications(updateRequest.getCertifications());
            log.debug("Certifications mises à jour.");
        }

        // Update domains if provided
        if (updateRequest.getDomaines() != null) {
            user.setDomaines(updateRequest.getDomaines());
            log.debug("Domaines mis à jour.");
        }

        // Save the updated user entity
        User updatedUser = userRepository.save(user);
        log.info("Profil utilisateur {} mis à jour avec succès.", userEmail);
        return updatedUser;
    }
}