package com.project.ChatBot.model;

public class ChatRequest {

    private String message;
    private Boolean usePdf;
    private String sessionId; // Add sessionId for chat history tracking

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getUsePdf() {
        return usePdf;
    }

    public void setUsePdf(Boolean usePdf) {
        this.usePdf = usePdf;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
