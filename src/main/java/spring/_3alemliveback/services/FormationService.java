package spring._3alemliveback.services;// package spring._3alemliveback.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import spring._3alemliveback.dto.formation.AvisRequest;
import spring._3alemliveback.dto.formation.FormationDTO; // Import FormationDTO
import spring._3alemliveback.dto.formation.FormationRequest;
import spring._3alemliveback.dto.formation.FormationResponseDTO;
import spring._3alemliveback.entities.Avis;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationStatus;
import spring._3alemliveback.enums.Role;
import spring._3alemliveback.exceptions.AccessDeniedException;
import spring._3alemliveback.exceptions.FormationNotFoundException; // Import FormationNotFoundException
import spring._3alemliveback.exceptions.InvalidOperationException;
import spring._3alemliveback.exceptions.UserNotFoundException;
import spring._3alemliveback.repo.AvisRepository;
import spring._3alemliveback.repo.FormationRepository;
import spring._3alemliveback.repo.UserRepository;
import spring._3alemliveback.services.GoogleMeetService;

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
     * Récupère un utilisateur par son email.
     */
    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new UserNotFoundException("L'email de l'utilisateur ne peut pas être vide.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé pour l'email : " + email));
    }

    @Transactional // Add Transactional if this method performs DB operations
    public Formation createFormation(FormationRequest formationRequest, String creatorEmail) {
        User currentUser = getUserByEmail(creatorEmail);

        if (currentUser.getRole() != Role.EXPERT) {
            throw new AccessDeniedException("Seuls les experts peuvent créer des formations");
        }
        if (!currentUser.isVerified() || !currentUser.isActive()) {
            throw new AccessDeniedException("Votre compte doit être vérifié et activé pour créer des formations");
        }

        String meetUrl = null;
        try {
            meetUrl = googleMeetService.createMeetLink(
                    formationRequest.getTitre(),
                    formationRequest.getDescription(),
                    formationRequest.getDateDebut(),
                    formationRequest.getDateFin()
            );
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Erreur lors de la création du lien Google Meet: " + e.getMessage());
            // Decide how to handle this error: re-throw, log and proceed, etc.
            // For now, we log and let meetUrl be null.
        }

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
                .urlMeet(meetUrl) // Set the generated meet URL
                .statut(FormationStatus.EN_ATTENTE)
                .formateur(currentUser)
                .build();
        return formationRepository.save(formation);
    }
    @Transactional
    public FormationDTO updateFormation(Long formationId, FormationRequest updatedFormationRequest, String userEmail) {
        User currentUser = getUserByEmail(userEmail); // Get the user performing the action

        Formation existingFormation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée avec l'ID : " + formationId));

        // Check if the current user is the creator (formateur) of the formation
        if (!existingFormation.getFormateur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette formation");
        }

        // You might add checks here if modification is only allowed for certain statuses
        // e.g., if (existingFormation.getStatut() != FormationStatus.EN_ATTENTE) {
        //     throw new InvalidOperationException("Vous ne pouvez modifier que les formations en attente.");
        // }

        // Update fields from the DTO to the entity
        // Use BeanUtils or manually set fields
        BeanUtils.copyProperties(updatedFormationRequest, existingFormation, "id", "statut", "formateur", "participants", "avis");
        // Manually set fields that need specific handling or are excluded by copyProperties
        existingFormation.setTitre(updatedFormationRequest.getTitre());
        existingFormation.setDescription(updatedFormationRequest.getDescription());
        existingFormation.setDateDebut(updatedFormationRequest.getDateDebut());
        existingFormation.setDateFin(updatedFormationRequest.getDateFin());
        existingFormation.setDuree(updatedFormationRequest.getDuree()); // Corrected typo if exists
        existingFormation.setNombreMaxParticipants(updatedFormationRequest.getNombreMaxParticipants());
        existingFormation.setPrix(updatedFormationRequest.getPrix());
        existingFormation.setCategorie(updatedFormationRequest.getCategorie());
        existingFormation.setImageFormation(updatedFormationRequest.getImageFormation());
        // Optionally update the Meet link if date/time changed? More complex.

        // Save the updated entity
        Formation updatedFormation = formationRepository.save(existingFormation);

        log.info("Formation with ID {} updated by user {}", formationId, userEmail);

        // Return the updated formation as a DTO
        return FormationDTO.fromEntity(updatedFormation);
    }

    // **NEW METHOD: Get Formation by ID**
    @Transactional(readOnly = true) // Use readOnly = true for read operations
    public FormationDTO getFormationById(Long formationId) {
        // Find the formation entity
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée avec l'ID : " + formationId));

        // You might add security checks here if only certain users can view details
        // For example:
        // User currentUser = ... // Get authenticated user
        // if (!formation.getFormateur().getId().equals(currentUser.getId()) && !formation.getParticipants().contains(currentUser)) {
        //    throw new AccessDeniedException("Vous n'êtes pas autorisé à voir les détails de cette formation");
        // }

        // Convert the entity to DTO within the transactional context
        return FormationDTO.fromEntity(formation);
    }


    public Formation approveFormation(Long formationId /*, String adminEmail */) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));
        formation.setStatut(FormationStatus.APPROUVEE);
        return formationRepository.save(formation);
    }

    public Formation rejectFormation(Long formationId /*, String adminEmail */) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));
        formation.setStatut(FormationStatus.REJETEE);
        return formationRepository.save(formation);
    }
    @Transactional
    public void deleteFormation(Long formationId, String userEmail) {
        User currentUser = getUserByEmail(userEmail); // Get the user performing the action

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée avec l'ID : " + formationId));

        // Check if the current user is the creator (formateur) of the formation
        if (!formation.getFormateur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cette formation");
        }

        // You might add checks here if deletion is only allowed for certain statuses
        // e.g., if (formation.getStatut() != FormationStatus.EN_ATTENTE) {
        //     throw new InvalidOperationException("Vous ne pouvez supprimer que les formations en attente.");
        // }

        formationRepository.delete(formation); // Delete the entity
        log.info("Formation with ID {} deleted by user {}", formationId, userEmail);
    }

    @Transactional // Add Transactional as it modifies the participants collection
    public Formation inscriptionFormation(Long formationId, String userEmail) {
        User currentUser = getUserByEmail(userEmail);
        if (currentUser.getRole() != Role.USER) {
            throw new AccessDeniedException("Seuls les apprenants peuvent s'inscrire aux formations");
        }

        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        if (formation.getStatut() != FormationStatus.APPROUVEE) {
            throw new AccessDeniedException("Cette formation n'est pas encore approuvée");
        }
        // Access participants collection within the transaction
        if (formation.getParticipants().size() >= formation.getNombreMaxParticipants()) {
            throw new AccessDeniedException("Cette formation a atteint son nombre maximum de participants");
        }
        // Access participants collection within the transaction
        if (formation.getParticipants().contains(currentUser)) {
            throw new AccessDeniedException("Vous êtes déjà inscrit à cette formation");
        }

        formation.getParticipants().add(currentUser);
        return formationRepository.save(formation);
    }

    /**
     * Récupère toutes les formations approuvées
     * @return Liste des formations sous forme de DTOs
     */
    @Transactional(readOnly = true)
    public List<FormationDTO> getAllApprovedFormations() {
        List<Formation> formations = formationRepository.findByStatut(FormationStatus.APPROUVEE);
        // DTO conversion inside the transaction
        return FormationDTO.fromEntities(formations);
    }

    @Transactional(readOnly = true) // Add Transactional
    public List<FormationResponseDTO> getAllPendingFormations() {
        List<Formation> formations = formationRepository.findByStatutWithFormateur(FormationStatus.EN_ATTENTE);
        log.debug("Nombre de formations trouvées: {}", formations.size());
        // DTO conversion inside the transaction
        return formations.stream()
                .map(FormationResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }


    /**
     * Récupère les formations créées par un expert
     * @param expertEmail Email de l'expert
     * @return Liste des formations sous forme de DTOs
     */
    @Transactional(readOnly = true) // Should be readOnly = true for read operations
    public List<FormationDTO> getMyFormations(String expertEmail) {
        User currentUser = getUserByEmail(expertEmail);

        if (currentUser.getRole() != Role.EXPERT) {
            throw new AccessDeniedException("Seuls les experts peuvent accéder à leurs formations");
        }

        List<Formation> formations = formationRepository.findByFormateur(currentUser);
        // DTO conversion inside the transaction
        return FormationDTO.fromEntities(formations);
    }

    /**
     * Récupère les formations auxquelles un apprenant est inscrit
     * @param userEmail Email de l'apprenant
     * @return Liste des formations sous forme de DTOs
     */
    @Transactional(readOnly = true) // Add Transactional
    public List<FormationDTO> getMyInscriptions(String userEmail) {
        User currentUser = getUserByEmail(userEmail);

        if (currentUser.getRole() != Role.USER) {
            throw new AccessDeniedException("Seuls les apprenants peuvent accéder à leurs inscriptions");
        }

        // Assuming findApprovedFormationsByParticipantId returns Formation entities
        List<Formation> formations = formationRepository.findApprovedFormationsByParticipantId(currentUser.getId());
        // DTO conversion inside the transaction
        return FormationDTO.fromEntities(formations);
    }

    @Transactional // Add Transactional as it modifies the status
    public Formation demarrerFormation(Long formationId, String formateurEmail) {
        User currentUser = getUserByEmail(formateurEmail);
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        if (!formation.getFormateur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas le formateur de cette formation");
        }
        if (formation.getStatut() != FormationStatus.APPROUVEE) {
            throw new InvalidOperationException("Seules les formations approuvées peuvent être démarrées");
        }

        formation.setStatut(FormationStatus.EN_COURS);
        return formationRepository.save(formation);
    }

    @Transactional // Add Transactional as it modifies the status
    public Formation terminerFormation(Long formationId, String formateurEmail) {
        User currentUser = getUserByEmail(formateurEmail);
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        if (!formation.getFormateur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas le formateur de cette formation");
        }
        if (formation.getStatut() != FormationStatus.EN_COURS) {
            throw new InvalidOperationException("Seules les formations en cours peuvent être terminées");
        }

        formation.setStatut(FormationStatus.TERMINEE);
        return formationRepository.save(formation);
    }

    @Transactional // Add Transactional as it creates a new Avis entity
    public Avis ajouterAvis(Long formationId, AvisRequest avisRequest, String userEmail) {
        User currentUser = getUserByEmail(userEmail);
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));

        if (formation.getStatut() != FormationStatus.TERMINEE) {
            throw new InvalidOperationException("Vous ne pouvez ajouter un avis que sur des formations terminées");
        }
        // Access participants collection within the transaction
        if (!formation.getParticipants().contains(currentUser)) {
            throw new AccessDeniedException("Vous n'êtes pas inscrit à cette formation");
        }
        Optional<Avis> existingAvis = avisRepository.findByFormationAndUtilisateur(formation, currentUser);
        if (existingAvis.isPresent()) {
            throw new InvalidOperationException("Vous avez déjà donné votre avis sur cette formation");
        }
        if (avisRequest.getNote() < 1 || avisRequest.getNote() > 5) {
            throw new InvalidOperationException("La note doit être comprise entre 1 et 5");
        }

        Avis avis = Avis.builder()
                .commentaire(avisRequest.getCommentaire())
                .note(avisRequest.getNote())
                .formation(formation)
                .utilisateur(currentUser)
                .dateCreation(LocalDateTime.now())
                .build();
        return avisRepository.save(avis);
    }

    @Transactional(readOnly = true) // Add Transactional
    public List<Avis> getAvisByFormation(Long formationId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new FormationNotFoundException("Formation non trouvée"));
        // Avis might be lazily loaded from Formation, ensure session is open if needed,
        // or fetch Avis directly. findByFormation is likely fine within transaction.
        return avisRepository.findByFormation(formation);
    }
}