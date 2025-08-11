package com.project.ChatBot.service;

import com.project.ChatBot.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RAGService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * Enhanced RAG response using Gemini API with PDF resource analysis
     */
    public ChatResponse getResponse(String question) {
        return getResponse(question, null);
    }

    /**
     * Get response using advanced RAG with Gemini API and PDF resources
     */
    public ChatResponse getResponse(String question, String sessionId) {
        try {
            System.out.println("🔍 RAGService: Processing question with Gemini API integration");

            // Get conversation context if session ID is provided
            String conversationContext = "";
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                conversationContext = chatHistoryService.buildConversationContext(sessionId, 3);
            }

            // Use GeminiService for intelligent processing with PDF resources
            String intelligentResponse = geminiService.getIntelligentResponse(question, conversationContext);

            // If Gemini service returns a valid response, use it
            if (intelligentResponse != null && !intelligentResponse.trim().isEmpty()) {
                System.out.println("✅ RAGService: Successfully generated Gemini-powered response");
                return new ChatResponse(intelligentResponse);
            }

            // Fallback to simple processing if Gemini fails
            System.out.println("⚠️ RAGService: Gemini response empty, using fallback");
            return getFallbackResponse(question);

        } catch (Exception e) {
            System.err.println("❌ RAGService Error: " + e.getMessage());
            e.printStackTrace();
            return new ChatResponse(
                "I apologize, but I'm experiencing technical difficulties. " +
                "Please try rephrasing your question or contact our People Operations team " +
                "at hcatindia.pops@healthcatalyst.com for immediate assistance."
            );
        }
    }

    /**
     * Enhanced HR query detection using Gemini's intelligence
     */
    public boolean isHRQuery(String question) {
        try {
            // Use simple keyword matching for quick HR detection
            return isSimpleHRQuery(question);

        } catch (Exception e) {
            System.err.println("Error in HR query detection: " + e.getMessage());
            // Fallback to simple keyword matching
            return isSimpleHRQuery(question);
        }
    }

    /**
     * Simple fallback HR query detection
     */
    private boolean isSimpleHRQuery(String question) {
        String lowerQuestion = question.toLowerCase();
        String[] hrKeywords = {
            "leave", "policy", "benefits", "employee", "hr", "vacation", "sick",
            "maternity", "paternity", "insurance", "salary", "work", "office",
            "disciplinary", "hybrid", "remote", "pf", "provident", "medical",
            "handbook", "pluxee", "meal card", "reimbursement", "workday"
        };

        for (String keyword : hrKeywords) {
            if (lowerQuestion.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all available document content for analysis
     */
    public String getAllDocumentContent() {
        try {
            Map<String, String> allContents = documentService.getAllDocumentContents();
            StringBuilder combinedContent = new StringBuilder();

            for (Map.Entry<String, String> entry : allContents.entrySet()) {
                combinedContent.append("=== ").append(entry.getKey()).append(" ===\n");
                combinedContent.append(entry.getValue()).append("\n\n");
            }

            return combinedContent.toString();
        } catch (Exception e) {
            System.err.println("Error getting all document content: " + e.getMessage());
            return "Error retrieving document content.";
        }
    }

    /**
     * Fallback response when Gemini API fails
     */
    private ChatResponse getFallbackResponse(String question) {
        try {
            // Try to find relevant content using DocumentService
            String relevantContent = findRelevantContent(question);

            if (relevantContent != null && !relevantContent.trim().isEmpty()) {
                // Generate a simple response with the found content
                String response = generateSimpleResponse(question, relevantContent);
                return new ChatResponse(response);
            }

            // If no relevant content found, return a helpful fallback
            return new ChatResponse(
                "I couldn't find specific information about your question in our HR documents. " +
                "For detailed assistance, please contact:\n\n" +
                "• **People Operations:** hcatindia.pops@healthcatalyst.com\n" +
                "• **IT Support:** hcatindia.itops@healthcatalyst.com\n" +
                "• **Payroll Queries:** mypay@pmry.in"
            );

        } catch (Exception e) {
            System.err.println("Error in fallback response: " + e.getMessage());
            return new ChatResponse(
                "I'm experiencing technical difficulties. Please contact our " +
                "People Operations team at hcatindia.pops@healthcatalyst.com for assistance."
            );
        }
    }

    /**
     * Find relevant content using DocumentService intelligence
     */
    private String findRelevantContent(String question) {
        String lowerQuestion = question.toLowerCase();

        // Determine intent and get relevant content
        String intent = determineIntent(lowerQuestion);
        return documentService.searchRelevantContent(question, intent);
    }

    /**
     * Simple intent determination for fallback scenarios
     */
    private String determineIntent(String lowerQuestion) {
        if (containsAny(lowerQuestion, "leave", "vacation", "time off", "pto", "holiday", "sick leave")) {
            return "leave_policy";
        }
        if (containsAny(lowerQuestion, "benefits", "insurance", "medical", "reimbursement", "pluxee", "meal card")) {
            return "benefits";
        }
        if (containsAny(lowerQuestion, "employee", "team", "staff", "who is", "contact", "role", "manager")) {
            return "employee_lookup";
        }
        if (containsAny(lowerQuestion, "maternity", "paternity", "parental", "pregnancy", "adoption")) {
            return "parental_leave";
        }
        if (containsAny(lowerQuestion, "disciplinary", "misconduct", "violation", "warning", "termination")) {
            return "disciplinary";
        }
        if (containsAny(lowerQuestion, "hybrid", "remote", "work from home", "wfh", "office")) {
            return "hybrid_work";
        }
        if (containsAny(lowerQuestion, "workday", "apply leave", "system", "portal", "application")) {
            return "workday_system";
        }
        if (containsAny(lowerQuestion, "internal", "hiring", "promotion", "transfer", "career", "lateral")) {
            return "internal_hiring";
        }

        return "general_hr";
    }

    /**
     * Check if text contains any of the given keywords
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a simple formatted response for fallback scenarios
     */
    private String generateSimpleResponse(String question, String content) {
        StringBuilder response = new StringBuilder();

        response.append("**Based on our HR documents:**\n\n");

        // Limit content length to avoid overwhelming responses
        String limitedContent = content.length() > 1500 ?
            content.substring(0, 1500) + "\n\n*[Response truncated for readability]*" :
            content;

        response.append(limitedContent);

        // Add helpful footer
        response.append("\n\n**Need more help?**\n");
        response.append("📧 **People Operations:** hcatindia.pops@healthcatalyst.com\n");
        response.append("💻 **IT Support:** hcatindia.itops@healthcatalyst.com\n");
        response.append("💰 **Payroll:** mypay@pmry.in");

        return response.toString();
    }

    /**
     * Get service status for monitoring
     */
    public String getServiceStatus() {
        try {
            // Test if DocumentService is working
            Map<String, String> allContents = documentService.getAllDocumentContents();
            boolean documentsLoaded = allContents != null && !allContents.isEmpty();

            // Test if GeminiService is working by checking if it's properly initialized
            boolean geminiWorking = geminiService != null;

            return String.format(
                "RAG Service Status:\n" +
                "• Gemini API: %s\n" +
                "• Document Service: %s\n" +
                "• PDF Resources: %d documents available",
                geminiWorking ? "✅ Active" : "❌ Error",
                documentsLoaded ? "✅ Active" : "❌ Error",
                documentsLoaded ? allContents.size() : 0
            );
        } catch (Exception e) {
            return "❌ RAG Service Error: " + e.getMessage();
        }
    }
}
