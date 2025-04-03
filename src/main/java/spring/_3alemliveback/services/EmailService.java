package spring._3alemliveback.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = "http://localhost:9094/api/v1/auth/verify?token=" + token;
        String subject = "Vérifiez votre compte";
        String content = "<p>Bonjour,</p>"
                + "<p>Merci de vous être inscrit. Cliquez sur le lien ci-dessous pour vérifier votre compte :</p>"
                + "<a href=\"" + verificationLink + "\">Vérifier mon compte</a>"
                + "<p>Si vous n'avez pas demandé cela, ignorez cet email.</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }
}
