package com.project.ChatBot.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String id;
    private String sessionId;
    private String sender; // "user" or "bot"
    private String message;
    private LocalDateTime timestamp;
    private boolean usedPdfContext;

    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String sessionId, String sender, String message, boolean usedPdfContext) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.message = message;
        this.usedPdfContext = usedPdfContext;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isUsedPdfContext() {
        return usedPdfContext;
    }

    public void setUsedPdfContext(boolean usedPdfContext) {
        this.usedPdfContext = usedPdfContext;
    }
}
