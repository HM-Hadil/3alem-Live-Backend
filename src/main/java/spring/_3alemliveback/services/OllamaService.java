package spring._3alemliveback.services;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import spring._3alemliveback.dto.chatbot.ChatbotIntent;
import spring._3alemliveback.dto.chatbot.OllamaRequest;
import spring._3alemliveback.dto.chatbot.OllamaResponse;

@Service
@RequiredArgsConstructor
public class OllamaService {
    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    private final RestTemplate restTemplate;

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String ollamaApiUrl;

    @Value("${ollama.model:llama2}")
    private String ollamaModel;

    public String askOllama(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            OllamaRequest request = OllamaRequest.builder()
                    .model(ollamaModel)
                    .prompt(prompt)
                    .temperature(0.3) // Valeur basse pour des réponses plus déterministes
                    .build();

            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request, headers);

            OllamaResponse response = restTemplate.postForObject(
                    ollamaApiUrl,
                    entity,
                    OllamaResponse.class);

            return response != null ? response.getResponse() : "Désolé, je n'ai pas pu obtenir de réponse.";
        } catch (Exception e) {
            log.error("Erreur lors de la communication avec Ollama", e);
            return "Désolé, une erreur s'est produite lors de la communication avec le service IA.";
        }
    }

    public ChatbotIntent detectIntent(String question) {
        // Prompt spécifique pour l'extraction d'intention
        String intentPrompt = "Tu es un analyseur d'intentions. Réponds uniquement par le nom d'une des intentions suivantes:\n" +
                "COUNT_FORMATEURS - Si la question demande le nombre de formateurs\n" +
                "LIST_DOMAINS - Si la question concerne la liste des domaines/catégories\n" +
                "COUNT_FORMATIONS_BY_CATEGORY - Si la question demande le nombre de formations dans une catégorie\n" +
                "TOP_RATED_FORMATIONS - Si la question concerne les formations les mieux notées\n" +
                "FIND_FORMATEURS_BY_DOMAIN - Si la question concerne les formateurs d'un domaine spécifique\n" +
                "GENERAL_INFORMATION - Pour les questions générales sur le système\n" +
                "UNKNOWN - Si tu ne peux pas déterminer l'intention\n\n" +
                "Question: \"" + question + "\"\n" +
                "Intention:";

        String intentResponse = askOllama(intentPrompt);
        try {
            return ChatbotIntent.valueOf(intentResponse.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Intention non reconnue: {}", intentResponse);
            return ChatbotIntent.UNKNOWN;
        }
    }
}