package com.project.ChatBot.service;

import com.project.ChatBot.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatHistoryService {

    // In-memory storage for chat sessions
    private final Map<String, List<ChatMessage>> chatSessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionLastActivity = new ConcurrentHashMap<>();

    // Context caching for performance
    private final Map<String, String> contextCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> lastContextSize = new ConcurrentHashMap<>();

    // Session timeout in milliseconds (30 minutes)
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000;

    public void saveChatMessage(ChatMessage message) {
        String sessionId = message.getSessionId();

        // Generate unique ID for the message
        message.setId(UUID.randomUUID().toString());

        // Get or create session chat history
        List<ChatMessage> sessionHistory = chatSessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        // Add message to session
        sessionHistory.add(message);

        // Update last activity
        sessionLastActivity.put(sessionId, System.currentTimeMillis());

        // Clear cached context when new message is added
        contextCache.remove(sessionId);

        // Cleanup expired sessions periodically
        if (sessionHistory.size() % 10 == 0) {
            cleanupExpiredSessions();
        }

        System.out.println("Saved message for session " + sessionId + ": " + message.getMessage());
    }

    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatSessions.getOrDefault(sessionId, new ArrayList<>());
    }

    public String buildConversationContext(String sessionId, int maxMessages) {
        List<ChatMessage> history = getChatHistory(sessionId);

        if (history.isEmpty()) {
            return "";
        }

        // Check if we can use cached context
        String cacheKey = sessionId;
        Integer lastSize = lastContextSize.get(sessionId);
        if (lastSize != null && lastSize == history.size() && contextCache.containsKey(cacheKey)) {
            return contextCache.get(cacheKey);
        }

        // Build context only with essential messages (reduced for speed)
        List<ChatMessage> recentMessages = history.stream()
                .skip(Math.max(0, history.size() - Math.min(maxMessages, 3))) // Reduced to 3 messages max
                .collect(Collectors.toList());

        StringBuilder context = new StringBuilder();

        // Build more concise context
        for (ChatMessage msg : recentMessages) {
            if ("user".equals(msg.getSender())) {
                context.append("User: ").append(truncateMessage(msg.getMessage(), 100)).append("\n");
            } else {
                context.append("Bot: ").append(truncateMessage(msg.getMessage(), 150)).append("\n");
            }
        }

        String finalContext = context.toString();

        // Cache the context
        contextCache.put(cacheKey, finalContext);
        lastContextSize.put(sessionId, history.size());

        return finalContext;
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null || message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }

    public void clearSession(String sessionId) {
        chatSessions.remove(sessionId);
        sessionLastActivity.remove(sessionId);
        contextCache.remove(sessionId);
        lastContextSize.remove(sessionId);
        System.out.println("Cleared session: " + sessionId);
    }

    public int getSessionMessageCount(String sessionId) {
        return chatSessions.getOrDefault(sessionId, new ArrayList<>()).size();
    }

    public boolean hasSession(String sessionId) {
        return chatSessions.containsKey(sessionId);
    }

    private void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredSessions = sessionLastActivity.entrySet().stream()
                .filter(entry -> currentTime - entry.getValue() > SESSION_TIMEOUT)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String sessionId : expiredSessions) {
            clearSession(sessionId);
        }

        if (!expiredSessions.isEmpty()) {
            System.out.println("Cleaned up " + expiredSessions.size() + " expired sessions");
        }
    }
}
