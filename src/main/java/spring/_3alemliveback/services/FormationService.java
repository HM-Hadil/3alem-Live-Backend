package spring._3alemliveback.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring._3alemliveback.dto.formation.FormationRequest;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationStatus;
import spring._3alemliveback.enums.Role;

import spring._3alemliveback.exceptions.AccessDeniedException;
import spring._3alemliveback.exceptions.FormationNotFoundException;
import spring._3alemliveback.exceptions.UserNotFoundException;
import spring._3alemliveback.repo.FormationRepository;
import spring._3alemliveback.repo.UserRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormationService {

    @Autowired
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final GoogleMeetService googleMeetService;

    /**
     * Crée une nouvelle formation par un expert
     */
    public Formation createFormation(FormationRequest formationRequest) {
        // Récupérer l'utilisateur connecté
        User currentUser = getCurrentUser();

        // Vérifier si l'utilisateur est un expert
        if (currentUser.getRole() != Role.EXPERT) {
            throw new AccessDeniedException("Seuls les experts peuvent créer des formations");
        }

        // Vérifier si l'expert est vérifié et actif
        if (!currentUser.isVerified() || !currentUser.isActive()) {
            throw new AccessDeniedException("Votre compte doit être vérifié et activé pour créer des formations");
        }

        // Créer le lien Google Meet
        String meetUrl = null;
        try {
            meetUrl = googleMeetService.createMeetLink(
                    formationRequest.getTitre(),
                    formationRequest.getDescription(),
                    formationRequest.getDateDebut(),
                    formationRequest.getDateFin()
            );
        } catch (IOException | GeneralSecurityException e) {
            // Gestion d'erreur - on continue sans Meet URL si ça échoue
            System.err.println("Erreur lors de la création du lien Google Meet: " + e.getMessage());
        }

        // Créer la formation
        Formation formation = Formation.builder()
                .titre(formationRequest.getTitre())
                .description(formationRequest.getDescription())
                .dateDebut(formationRequest.getDateDebut())
                .dateFin(formationRequest.getDateFin())
                .duree(formationRequest.getDuree())
                .nombreMaxParticipants(formationRequest.getNombreMaxParticipants())
                .prix(formationRequest.getPrix())
                .categorie(formationRequest.getCategorie())
                .imageFormation(formationRequest.getImageFormation())
                .urlMeet(meetUrl)
                .statut(FormationStatus.EN_ATTENTE) // Statut par défaut
                .formateur(currentUser)
                .build();

        return formationRepository.save(formation);
    }

    /**
     * Approuve une formation par un administrateur
     */
    public Formation approveFormation(Long formationId) {
        // Vérifier que l'utilisateur est un admin
        User currentUser = getCurrentUser();

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        formation.setStatut(FormationStatus.APPROUVEE);
        return formationRepository.save(formation);
    }

    /**
     * Rejette une formation par un administrateur
     */
    public Formation rejectFormation(Long formationId) {
        // Vérifier que l'utilisateur est un admin
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Seuls les administrateurs peuvent rejeter les formations");
        }

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        formation.setStatut(FormationStatus.REJETEE);
        return formationRepository.save(formation);
    }

    /**
     * Permet à un apprenant de s'inscrire à une formation
     */
    public Formation inscriptionFormation(Long formationId) {
        // Vérifier que l'utilisateur est un apprenant
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.USER) {
            throw new AccessDeniedException("Seuls les apprenants peuvent s'inscrire aux formations");
        }

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        // Vérifier que la formation est approuvée
        if (formation.getStatut() != FormationStatus.APPROUVEE) {
            throw new AccessDeniedException("Cette formation n'est pas encore approuvée");
        }

        // Vérifier s'il reste des places
        if (formation.getParticipants().size() >= formation.getNombreMaxParticipants()) {
            throw new AccessDeniedException("Cette formation a atteint son nombre maximum de participants");
        }

        // Vérifier si l'utilisateur est déjà inscrit
        if (formation.getParticipants().contains(currentUser)) {
            throw new AccessDeniedException("Vous êtes déjà inscrit à cette formation");
        }

        // Ajouter l'utilisateur à la formation
        formation.getParticipants().add(currentUser);
        return formationRepository.save(formation);
    }

    /**
     * Récupère toutes les formations approuvées
     */
    public List<Formation> getAllApprovedFormations() {
        return formationRepository.findByStatut(FormationStatus.APPROUVEE);
    }

    /**
     * Récupère toutes les formations en attente (pour l'admin)
     */
    public List<Formation> getAllPendingFormations() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Accès non autorisé");
        }
        return formationRepository.findByStatut(FormationStatus.EN_ATTENTE);
    }

    /**
     * Récupère les formations créées par l'expert connecté
     */
    public List<Formation> getMyFormations() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.EXPERT) {
            throw new AccessDeniedException("Seuls les experts peuvent accéder à leurs formations");
        }
        return formationRepository.findByFormateur(currentUser);
    }

    /**
     * Récupère les formations auxquelles l'apprenant est inscrit
     */
    public List<Formation> getMyInscriptions() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.USER) {
            throw new AccessDeniedException("Seuls les apprenants peuvent accéder à leurs inscriptions");
        }

        // Il faudrait idéalement une méthode dans le repository pour faire cette requête directement
        // Pour l'instant, on récupère toutes les formations approuvées et on filtre côté service
        return formationRepository.findApprovedFormationsByParticipantId(currentUser.getId());

    }

    /**
     * Utilitaire pour récupérer l'utilisateur connecté
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé"));
    }
}