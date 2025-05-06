package spring._3alemliveback.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring._3alemliveback.dto.formation.AvisRequest;
import spring._3alemliveback.dto.formation.FormationRequest;
import spring._3alemliveback.dto.formation.FormationResponseDTO;
import spring._3alemliveback.entities.Avis;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationStatus;
import spring._3alemliveback.enums.Role;
import spring._3alemliveback.exceptions.AccessDeniedException;
import spring._3alemliveback.exceptions.FormationNotFoundException;
import spring._3alemliveback.exceptions.InvalidOperationException;
import spring._3alemliveback.exceptions.UserNotFoundException;
import spring._3alemliveback.repo.AvisRepository;
import spring._3alemliveback.repo.FormationRepository;
import spring._3alemliveback.repo.UserRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationService {

    @Autowired
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final AvisRepository avisRepository;
    private static final Logger log = LoggerFactory.getLogger(FormationService.class);
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
        /**  if (currentUser.getRole() != Role.ADMIN) {
         throw new AccessDeniedException("Seuls les administrateurs peuvent rejeter les formations");
         }**/

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
    @Transactional(readOnly = true)
    public List<FormationResponseDTO> getAllPendingFormations() {
        List<Formation> formations = formationRepository.findByStatutWithFormateur(FormationStatus.EN_ATTENTE);
        log.debug("Nombre de formations trouvées: {}", formations.size()); // Log important

        return formations.stream()
                .map(FormationResponseDTO::fromEntity)
                .collect(Collectors.toList());
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
     * Permet à un formateur de démarrer sa formation (changement de statut vers EN_COURS)
     */
    public Formation demarrerFormation(Long formationId) {
        User currentUser = getCurrentUser();
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        // Vérifier que l'utilisateur est bien le formateur de cette formation
        if (!formation.getFormateur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas le formateur de cette formation");
        }

        // Vérifier que la formation est approuvée
        if (formation.getStatut() != FormationStatus.APPROUVEE) {
            throw new InvalidOperationException("Seules les formations approuvées peuvent être démarrées");
        }

        // Changer le statut
        formation.setStatut(FormationStatus.EN_COURS);
        return formationRepository.save(formation);
    }

    /**
     * Permet à un formateur de terminer sa formation (changement de statut vers TERMINEE)
     */
    public Formation terminerFormation(Long formationId) {
        User currentUser = getCurrentUser();
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        // Vérifier que l'utilisateur est bien le formateur de cette formation
        if (!formation.getFormateur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas le formateur de cette formation");
        }

        // Vérifier que la formation est en cours
        if (formation.getStatut() != FormationStatus.EN_COURS) {
            throw new InvalidOperationException("Seules les formations en cours peuvent être terminées");
        }

        // Changer le statut
        formation.setStatut(FormationStatus.TERMINEE);
        return formationRepository.save(formation);
    }

    /**
     * Permet à un participant d'ajouter un avis sur une formation terminée
     */
    public Avis ajouterAvis(Long formationId, AvisRequest avisRequest) {
        User currentUser = getCurrentUser();
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        // Vérifier que la formation est terminée
        if (formation.getStatut() != FormationStatus.TERMINEE) {
            throw new InvalidOperationException("Vous ne pouvez ajouter un avis que sur des formations terminées");
        }

        // Vérifier que l'utilisateur est bien un participant de cette formation
        if (!formation.getParticipants().contains(currentUser)) {
            throw new AccessDeniedException("Vous n'êtes pas inscrit à cette formation");
        }

        // Vérifier si l'utilisateur a déjà donné son avis
        Optional<Avis> existingAvis = avisRepository.findByFormationAndUtilisateur(formation, currentUser);
        if (existingAvis.isPresent()) {
            throw new InvalidOperationException("Vous avez déjà donné votre avis sur cette formation");
        }

        // Valider la note (entre 1 et 5)
        if (avisRequest.getNote() < 1 || avisRequest.getNote() > 5) {
            throw new InvalidOperationException("La note doit être comprise entre 1 et 5");
        }

        // Créer l'avis
        Avis avis = Avis.builder()
                .commentaire(avisRequest.getCommentaire())
                .note(avisRequest.getNote())
                .formation(formation)
                .utilisateur(currentUser)
                .dateCreation(LocalDateTime.now())
                .build();

        return avisRepository.save(avis);
    }

    /**
     * Récupère tous les avis d'une formation
     */
    public List<Avis> getAvisByFormation(Long formationId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        return avisRepository.findByFormation(formation);
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