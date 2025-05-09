package spring._3alemliveback.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationToken(String token);

    boolean existsByEmail(String email);
    Long countByRole(Role role);
    // Find experts who have verified their email but haven't been activated by admin
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isVerified = true AND u.isActive = false")
    List<User> findByRoleAndIsVerifiedTrueAndIsActiveFalse(Role role);
    List<User> findByRoleAndIsVerifiedTrue(Role role);
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isVerified = true AND u.isActive = true")
    List<User> findByRoleAndIsVerifiedTrueAndIsActiveTrue(Role role);
}