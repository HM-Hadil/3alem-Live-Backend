package spring._3alemliveback.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring._3alemliveback.entities.Avis;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisRepository extends JpaRepository<Avis, Long> {
    List<Avis> findByFormation(Formation formation);
    List<Avis> findByUtilisateur(User utilisateur);
    Optional<Avis> findByFormationAndUtilisateur(Formation formation, User utilisateur);
}