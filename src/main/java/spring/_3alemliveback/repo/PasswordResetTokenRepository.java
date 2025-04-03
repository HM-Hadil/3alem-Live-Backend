package spring._3alemliveback.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import spring._3alemliveback.entities.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
}
