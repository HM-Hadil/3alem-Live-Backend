package spring._3alemliveback.repo;

import org.springframework.data.repository.query.Param;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationCategory;
import spring._3alemliveback.enums.FormationStatus;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {
    List<Formation> findByFormateur(User formateur);
    List<Formation> findByStatut(FormationStatus statut);
    @Query("SELECT f FROM Formation f LEFT JOIN FETCH f.formateur WHERE f.statut = :statut")
    List<Formation> findByStatutWithFormateur(@Param("statut") FormationStatus statut);
    List<Formation> findByFormateurAndStatut(User formateur, FormationStatus statut);
    @Query(value = "SELECT f.* FROM formations f " +
            "JOIN formation_participants fp ON f.id = fp.formation_id " +
            "WHERE f.statut = 'APPROUVEE' AND fp.user_id = :userId",
            nativeQuery = true)
    List<Formation> findApprovedFormationsByParticipantId(@Param("userId") Long userId);

    // Compter les formations par catégorie
    Long countByCategorie(FormationCategory categorie);

    // Trouver les formations les mieux notées
    @Query("SELECT f, AVG(a.note) as avgRating FROM Formation f JOIN f.avis a GROUP BY f ORDER BY avgRating DESC LIMIT :limit")
    List<Object[]> findTopRatedFormations(@Param("limit") int limit);

    // Trouver les formateurs par catégorie
    @Query("SELECT DISTINCT f.formateur FROM Formation f WHERE f.categorie = :categorie")
    List<User> findFormateursByCategory(@Param("categorie") FormationCategory categorie);
}
