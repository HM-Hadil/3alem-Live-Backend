package spring._3alemliveback.dto.formation;

import lombok.Builder;
import lombok.Data;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.enums.FormationCategory;
import spring._3alemliveback.enums.FormationStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class FormationResponseDTO {
    private Long id;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer duree;
    private Integer nombreMaxParticipants;
    private Double prix;
    private String urlMeet;
    private FormationCategory categorie;
    private FormationStatus statut;
    private Long formateurId;
    private String formateurNom;
    private Integer participantsCount;

    public static FormationResponseDTO fromEntity(Formation formation) {
        return FormationResponseDTO.builder()
                .id(formation.getId())
                .titre(formation.getTitre())
                .description(formation.getDescription())
                .dateDebut(formation.getDateDebut())
                .dateFin(formation.getDateFin())
                .duree(formation.getDuree())
                .nombreMaxParticipants(formation.getNombreMaxParticipants())
                .prix(formation.getPrix())
                .urlMeet(formation.getUrlMeet())
                .categorie(formation.getCategorie())
                .statut(formation.getStatut())
                .formateurId(formation.getFormateur() != null ? formation.getFormateur().getId() : null)
                .formateurNom(formation.getFormateur() != null ?
                        formation.getFormateur().getNom() + " " + formation.getFormateur().getPrenom() : null)
                .participantsCount(formation.getParticipants() != null ? formation.getParticipants().size() : 0)
                .build();
    }
}