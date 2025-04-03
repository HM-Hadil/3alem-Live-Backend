package spring._3alemliveback.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import spring._3alemliveback.entities.PasswordResetToken;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.repo.PasswordResetTokenRepository;
import spring._3alemliveback.repo.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Utilisateur introuvable");
        }

        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);

        String resetUrl = "http://localhost:9094/reset-password?token=" + token;
        sendEmail(user.getEmail(), "Réinitialisation de mot de passe",
                "Cliquez sur le lien suivant pour réinitialiser votre mot de passe : " + resetUrl);
    }

    private void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public boolean validateToken(String token) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        return tokenOptional.isPresent() && !tokenOptional.get().isExpired();
    }

    public void updatePassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(token);
        if (tokenOptional.isEmpty() || tokenOptional.get().isExpired()) {
            throw new IllegalArgumentException("Token invalide ou expiré");
        }

        User user = tokenOptional.get().getUser();
        String encodedPassword = new BCryptPasswordEncoder().encode(newPassword);

        user.setPassword(encodedPassword); // Assurez-vous d'encoder le mot de passe
        userRepository.save(user);

        // Supprime le token après utilisation
        tokenRepository.delete(tokenOptional.get());
    }
}
