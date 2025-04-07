package spring._3alemliveback.dto.formation;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import spring._3alemliveback.enums.FormationCategory;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FormationRequest {
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer duree;
    private Integer nombreMaxParticipants;
    private Double prix;
    private FormationCategory categorie;
    private byte[] imageFormation;
}