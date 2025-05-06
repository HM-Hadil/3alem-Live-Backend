package spring._3alemliveback.dto.chatbot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {
    private String model; // Nom du modèle à utiliser
    private String prompt; // La requête à traiter
    private Double temperature; // Contrôle de la créativité (0.0 - 1.0)
}