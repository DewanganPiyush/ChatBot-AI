package com.project.ChatBot.controller;

import com.project.ChatBot.model.ChatRequest;
import com.project.ChatBot.model.ChatResponse;
import com.project.ChatBot.model.ChatMessage;
import com.project.ChatBot.service.DocumentService;
import com.project.ChatBot.service.ChatHistoryService;
import com.project.ChatBot.service.RAGService;
import com.project.ChatBot.service.IntelligentChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RAGService ragService;

    @Autowired
    private IntelligentChatbotService intelligentChatbotService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            String message = request.getMessage();
            String sessionId = request.getSessionId();

            // Generate session ID if not provided
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }

            // Save user message to chat history
            ChatMessage userMessage = new ChatMessage(sessionId, "user", message, false);
            chatHistoryService.saveChatMessage(userMessage);

            System.out.println("🚀 Processing intelligent query: " + message);
            System.out.println("📱 Session ID: " + sessionId);

            // Use the new intelligent chatbot service for ALL queries

            String response = intelligentChatbotService.processIntelligentQuery(message, sessionId);

            // Save bot response to chat history
            ChatMessage botMessage = new ChatMessage(sessionId, "bot", response, false);
            chatHistoryService.saveChatMessage(botMessage);

            return ResponseEntity.ok(new ChatResponse(response, sessionId));

        } catch (Exception e) {
            System.err.println("❌ Error in chat controller: " + e.getMessage());
            e.printStackTrace();

            String errorResponse = "I apologize, but I'm experiencing some technical difficulties right now. " +
                                 "Please try again in a moment, or contact HR directly at hr@healthcatalyst.com for immediate assistance. 🔧";

            return ResponseEntity.ok(new ChatResponse(errorResponse));
        }
    }

    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String sessionId) {
        try {
            return ResponseEntity.ok(chatHistoryService.getChatHistory(sessionId));
        } catch (Exception e) {
            return ResponseEntity.ok("[]");
        }
    }

    @DeleteMapping("/chat/history/{sessionId}")
    public ResponseEntity<?> clearChatHistory(@PathVariable String sessionId) {
        try {
            chatHistoryService.clearSession(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.ok().build();
        }
    }
}
