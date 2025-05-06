package spring._3alemliveback.dto.formation;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvisRequest {
    private String commentaire;
    private Integer note; // Rating out of 5
}