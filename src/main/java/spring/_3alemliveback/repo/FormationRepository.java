package spring._3alemliveback.repo;

import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {
    List<Formation> findByFormateur(User formateur);
    List<Formation> findByStatut(FormationStatus statut);
    List<Formation> findByFormateurAndStatut(User formateur, FormationStatus statut);
}
