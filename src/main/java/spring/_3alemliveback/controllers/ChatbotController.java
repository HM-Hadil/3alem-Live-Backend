package spring._3alemliveback.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring._3alemliveback.dto.chatbot.ChatbotRequest;
import spring._3alemliveback.dto.chatbot.ChatbotResponse;
import spring._3alemliveback.services.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> askQuestion(@RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.processQuestion(request);
        return ResponseEntity.ok(response);
    }
}