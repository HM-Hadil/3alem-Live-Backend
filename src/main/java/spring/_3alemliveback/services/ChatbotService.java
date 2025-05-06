package spring._3alemliveback.services;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spring._3alemliveback.dto.chatbot.ChatbotIntent;
import spring._3alemliveback.dto.chatbot.ChatbotRequest;
import spring._3alemliveback.dto.chatbot.ChatbotResponse;
import spring._3alemliveback.entities.Formation;
import spring._3alemliveback.entities.User;
import spring._3alemliveback.enums.FormationCategory;
import spring._3alemliveback.enums.Role;
import spring._3alemliveback.repo.AvisRepository;
import spring._3alemliveback.repo.FormationRepository;
import spring._3alemliveback.repo.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private final OllamaService ollamaService;
    private final FormationRepository formationRepository;
    private final UserRepository userRepository;
    private final AvisRepository avisRepository;

    public ChatbotResponse processQuestion(ChatbotRequest request) {
        String question = request.getQuestion();
        ChatbotIntent intent = ollamaService.detectIntent(question);

        log.info("Question reçue: '{}', intention détectée: {}", question, intent);

        // Extraction d'informations selon l'intention
        Object data = null;
        String answer;

        switch (intent) {
            case COUNT_FORMATEURS:
                data = countFormateurs();
                answer = formatCountFormateursResponse((Long) data);
                break;

            case LIST_DOMAINS:
                data = listDomains();
                answer = formatListDomainsResponse((List<String>) data);
                break;

            case COUNT_FORMATIONS_BY_CATEGORY:
                FormationCategory category = extractCategoryFromQuestion(question);
                data = countFormationsByCategory(category);
                answer = formatCountFormationsByCategoryResponse(category, (Long) data);
                break;

            case TOP_RATED_FORMATIONS:
                data = getTopRatedFormations();
                answer = formatTopRatedFormationsResponse((List<Map<String, Object>>) data);
                break;

            case FIND_FORMATEURS_BY_DOMAIN:
                FormationCategory domainCategory = extractCategoryFromQuestion(question);
                data = findFormateursByDomain(domainCategory);
                answer = formatFormateursByDomainResponse(domainCategory, (List<User>) data);
                break;

            case GENERAL_INFORMATION:
                // Utilisez Ollama directement pour les questions générales
                answer = ollamaService.askOllama(
                        "Réponds à cette question concernant une plateforme de formation en ligne: " + question);
                break;

            case UNKNOWN:
            default:
                answer = "Je ne suis pas sûr de comprendre votre question. Pourriez-vous la reformuler ou être plus précis?";
        }

        return ChatbotResponse.builder()
                .answer(answer)
                .detectedIntent(intent)
                .data(data)
                .build();
    }

    // Méthodes d'extraction de données

    private Long countFormateurs() {
        return userRepository.countByRole(Role.EXPERT);
    }

    private List<String> listDomains() {
        return List.of(FormationCategory.values())
                .stream()
                .map(FormationCategory::name)
                .collect(Collectors.toList());
    }

    private Long countFormationsByCategory(FormationCategory category) {
        return formationRepository.countByCategorie(category);
    }

    private List<Map<String, Object>> getTopRatedFormations() {
        // Cette requête nécessite probablement une méthode personnalisée dans le repository
        // ou une implémentation JPA spécifique pour calculer la moyenne des avis
        List<Object[]> results = formationRepository.findTopRatedFormations(5); // Top 5

        return results.stream().map(result -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ((Formation) result[0]).getId());
            map.put("titre", ((Formation) result[0]).getTitre());
            map.put("note_moyenne", (Double) result[1]);
            return map;
        }).collect(Collectors.toList());
    }

    private List<User> findFormateursByDomain(FormationCategory category) {
        return formationRepository.findFormateursByCategory(category);
    }

    // Méthodes d'extraction d'informations depuis la question

    private FormationCategory extractCategoryFromQuestion(String question) {
        // Prompt spécifique pour l'extraction de catégorie
        String categoryPrompt = "Tu es un extracteur de catégories de formations. Identifie la catégorie mentionnée dans la question suivante et réponds uniquement par l'une des valeurs suivantes: " +
                String.join(", ", listDomains()) + ".\n" +
                "Si aucune catégorie n'est mentionnée ou si tu n'es pas sûr, réponds UNKNOWN.\n\n" +
                "Question: \"" + question + "\"\n" +
                "Catégorie:";

        String categoryResponse = ollamaService.askOllama(categoryPrompt).trim().toUpperCase();

        try {
            return FormationCategory.valueOf(categoryResponse);
        } catch (IllegalArgumentException e) {
            log.warn("Catégorie non reconnue: {}", categoryResponse);
            return null;
        }
    }

    // Méthodes de formatage des réponses

    private String formatCountFormateursResponse(Long count) {
        return "Il y a actuellement " + count + " formateurs sur la plateforme.";
    }

    private String formatListDomainsResponse(List<String> domains) {
        return "Les domaines de formation disponibles sont : " + String.join(", ", domains) + ".";
    }

    private String formatCountFormationsByCategoryResponse(FormationCategory category, Long count) {
        if (category == null) {
            return "Je n'ai pas pu identifier la catégorie dans votre question. Pourriez-vous préciser?";
        }
        return "Il y a " + count + " formations dans la catégorie " + category.name() + ".";
    }

    private String formatTopRatedFormationsResponse(List<Map<String, Object>> formations) {
        if (formations.isEmpty()) {
            return "Il n'y a pas encore de formations avec des avis.";
        }

        StringBuilder response = new StringBuilder("Les formations les mieux notées sont :\n");
        for (int i = 0; i < formations.size(); i++) {
            Map<String, Object> formation = formations.get(i);
            response.append(i + 1)
                    .append(". ")
                    .append(formation.get("titre"))
                    .append(" (Note: ")
                    .append(formation.get("note_moyenne"))
                    .append("/5)\n");
        }

        return response.toString();
    }

    private String formatFormateursByDomainResponse(FormationCategory category, List<User> formateurs) {
        if (category == null) {
            return "Je n'ai pas pu identifier le domaine dans votre question. Pourriez-vous préciser?";
        }

        if (formateurs.isEmpty()) {
            return "Il n'y a pas encore de formateurs pour le domaine " + category.name() + ".";
        }

        StringBuilder response = new StringBuilder("Les formateurs du domaine " + category.name() + " sont :\n");
        for (int i = 0; i < formateurs.size(); i++) {
            User formateur = formateurs.get(i);
            response.append(i + 1)
                    .append(". ")
                    .append(formateur.getNom())
                    .append(" ")
                    .append(formateur.getPrenom())
                    .append("\n");
        }

        return response.toString();
    }
}