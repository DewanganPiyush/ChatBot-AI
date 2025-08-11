package com.project.ChatBot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class IntelligentChatbotService {

    // Updated to use Gemini API key from application.properties
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private GeminiService geminiService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Conversation context cache
    private final Map<String, List<String>> conversationHistory = new HashMap<>();

    /**
     * MAIN INTELLIGENT CHATBOT METHOD - Enhanced with Gemini API
     * Gemini first interprets user query, then searches PDF resources, then generates optimized response
     */
    public String processIntelligentQuery(String userMessage, String sessionId) {
        try {
            System.out.println("🚀 Processing intelligent query with Gemini API: " + userMessage);

            // Step 1: Build conversation context from chat history
            String conversationContext = buildConversationContext(sessionId);

            // Step 2: Use enhanced GeminiService for complete processing
            // This service now handles: intent analysis → PDF resource search → response generation
            String response = geminiService.getIntelligentResponse(userMessage, conversationContext);

            // Step 3: Update conversation history for context
            updateConversationHistory(sessionId, userMessage, response);

            System.out.println("✅ Generated intelligent response using Gemini + PDF resources");
            return response;

        } catch (Exception e) {
            System.err.println("❌ Error in intelligent processing: " + e.getMessage());
            e.printStackTrace();
            return generateEmergencyFallbackResponse(userMessage);
        }
    }

    /**
     * Build conversation context from recent chat history
     */
    private String buildConversationContext(String sessionId) {
        try {
            List<String> recentMessages = conversationHistory.getOrDefault(sessionId, new ArrayList<>());

            if (recentMessages.isEmpty()) {
                return "";
            }

            // Get last 4 messages for context (2 exchanges)
            int startIndex = Math.max(0, recentMessages.size() - 4);
            List<String> contextMessages = recentMessages.subList(startIndex, recentMessages.size());

            StringBuilder context = new StringBuilder();
            for (int i = 0; i < contextMessages.size(); i += 2) {
                if (i + 1 < contextMessages.size()) {
                    context.append("User: ").append(contextMessages.get(i)).append("\n");
                    context.append("Assistant: ").append(contextMessages.get(i + 1)).append("\n");
                }
            }

            return context.toString();

        } catch (Exception e) {
            System.err.println("⚠️ Error building conversation context: " + e.getMessage());
            return "";
        }
    }

    /**
     * Update conversation history for context building
     */
    private void updateConversationHistory(String sessionId, String userMessage, String botResponse) {
        try {
            List<String> messages = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());

            // Add user message and bot response
            messages.add(userMessage);
            messages.add(botResponse);

            // Keep only last 10 messages (5 exchanges) to prevent memory issues
            if (messages.size() > 10) {
                messages = messages.subList(messages.size() - 10, messages.size());
                conversationHistory.put(sessionId, messages);
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error updating conversation history: " + e.getMessage());
        }
    }

    /**
     * Emergency fallback response when all systems fail
     */
    private String generateEmergencyFallbackResponse(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        // Simple pattern matching for emergency responses
        if (lowerMessage.contains("hello") || lowerMessage.contains("hi") || lowerMessage.contains("hey")) {
            return "Hello! I'm experiencing some technical difficulties, but I'm here to help. Please try asking your question again, or contact HR directly for immediate assistance.";
        }

        if (lowerMessage.contains("leave") || lowerMessage.contains("vacation")) {
            return "For leave-related inquiries, please refer to the Employee Handbook or contact HR directly. I apologize for the technical difficulties.";
        }

        if (lowerMessage.contains("benefit") || lowerMessage.contains("insurance")) {
            return "For benefits information, please check your Benefits Handbook or contact HR. I'm currently experiencing technical issues.";
        }

        if (lowerMessage.contains("piyush") || lowerMessage.contains("employee")) {
            return "For employee information, please contact HR directly. I'm unable to access the employee directory at the moment due to technical issues.";
        }

        return "I apologize, but I'm experiencing technical difficulties and cannot process your request right now. " +
               "Please contact HR directly at hr@healthcatalyst.com for immediate assistance, or try again in a few moments.";
    }

    /**
     * Legacy method for backward compatibility - now delegates to GeminiService
     */
    @Deprecated
    public String processQuery(String userMessage, String sessionId) {
        return processIntelligentQuery(userMessage, sessionId);
    }

    /**
     * Health check method
     */
    public boolean isServiceHealthy() {
        try {
            // Test basic functionality
            String testResponse = geminiService.getIntelligentResponse("Hello", "");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            System.err.println("❌ Service health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get service status information
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            status.put("service", "IntelligentChatbotService");
            status.put("gemini_api_configured", geminiApiKey != null && !geminiApiKey.isEmpty());
            status.put("document_service_available", documentService != null);
            status.put("healthy", isServiceHealthy());
            status.put("active_sessions", conversationHistory.size());
            status.put("timestamp", new Date());

        } catch (Exception e) {
            status.put("error", e.getMessage());
            status.put("healthy", false);
        }

        return status;
    }

    /**
     * Clear conversation history for a session
     */
    public void clearSessionHistory(String sessionId) {
        conversationHistory.remove(sessionId);
        System.out.println("🗑️ Cleared conversation history for session: " + sessionId);
    }

    /**
     * Clear all conversation histories (admin function)
     */
    public void clearAllHistories() {
        conversationHistory.clear();
        System.out.println("🗑️ Cleared all conversation histories");
    }
}
