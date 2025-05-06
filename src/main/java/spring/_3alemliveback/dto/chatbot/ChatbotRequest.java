package spring._3alemliveback.dto.chatbot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    private String question;
    private String userId; // Optionnel, pour personnalisation
}
