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
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash-latest}")
    private String model;

    @Value("${gemini.max.tokens:2048}")
    private Integer maxTokens;

    @Value("${gemini.temperature:0.7}")
    private Double temperature;

    @Autowired
    private DocumentService documentService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";

    /**
     * Main method - Intelligent Company Assistant with Gemini API
     * Workflow: Intent Understanding → PDF Content Search → Optimized Response Generation
     */
    public String getIntelligentResponse(String userQuestion, String conversationContext) {
        try {
            System.out.println("🤖 Intelligent Company Assistant processing: " + userQuestion);

            // Step 1: Quick greeting/small talk detection - respond instantly
            String greetingResponse = handleGreetingOrSmallTalk(userQuestion);
            if (greetingResponse != null) {
                System.out.println("👋 Quick greeting response provided");
                return greetingResponse;
            }

            // Step 2: Use Gemini to deeply understand user's intent and what they truly need
            String userIntent = analyzeUserIntentAndNeeds(userQuestion);
            System.out.println("🧠 Gemini Intent Understanding: " + userIntent);

            // Step 3: Search PDF content for relevant information
            String relevantPdfContent = searchPdfContentWithGemini(userQuestion, userIntent);

            if (relevantPdfContent != null && !relevantPdfContent.trim().isEmpty() &&
                !relevantPdfContent.contains("NO_RELEVANT_INFO_FOUND")) {

                // Step 4a: Relevant information exists - combine with Gemini understanding
                System.out.println("📄 Found relevant PDF content - generating optimized response");
                return generateOptimizedResponseWithPdfContent(userQuestion, relevantPdfContent, conversationContext, userIntent);

            } else {
                // Step 4b: No relevant information in PDFs - handle with Gemini API alone
                System.out.println("🧠 No relevant PDF content - using Gemini general knowledge");
                return generateGeminiStandaloneResponse(userQuestion, conversationContext);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in intelligent assistant processing: " + e.getMessage());
            e.printStackTrace();
            return generateGeminiErrorResponse(userQuestion);
        }
    }

    /**
     * Handle greetings and small talk instantly without PDF searches
     */
    private String handleGreetingOrSmallTalk(String userQuestion) {
        String lowerQuestion = userQuestion.toLowerCase().trim();
        
        // Greeting patterns
        if (lowerQuestion.matches("^(hi|hello|hey|good morning|good afternoon|good evening).*") || 
            lowerQuestion.equals("hi") || lowerQuestion.equals("hello") || lowerQuestion.equals("hey")) {
            return "Hello! I'm your Health Catalyst HR assistant. How can I help you today?";
        }
        
        // Farewell patterns
        if (lowerQuestion.matches(".*(bye|goodbye|see you|take care|thanks|thank you).*") ||
            lowerQuestion.equals("bye") || lowerQuestion.equals("thanks") || lowerQuestion.equals("thank you")) {
            return "You're welcome! Have a great day! Feel free to ask if you need any HR assistance.";
        }
        
        // How are you patterns
        if (lowerQuestion.contains("how are you") || lowerQuestion.contains("what's up") || 
            lowerQuestion.contains("whats up")) {
            return "I'm doing great, thank you! Ready to help with any HR questions or information you need.";
        }
        
        // Very short queries (1-3 words that are casual)
        if (lowerQuestion.length() <= 15 && 
            (lowerQuestion.contains("ok") || lowerQuestion.contains("okay") || 
             lowerQuestion.contains("cool") || lowerQuestion.contains("nice"))) {
            return "Is there anything specific I can help you with regarding Health Catalyst policies or procedures?";
        }
        
        return null; // Not a greeting/small talk, proceed with normal processing
    }

    /**
     * Step 2: Analyze user's intent using Gemini to understand what they truly need
     */
    private String analyzeUserIntentAndNeeds(String userQuestion) {
        try {
            JSONObject requestBody = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", String.format(
                "You are an intelligent intent analyzer for a company assistant. Your task is to deeply understand what the user truly needs.\n\n" +
                "User Question: '%s'\n\n" +
                "Analyze this question and provide:\n" +
                "1. What is the user really asking for?\n" +
                "2. What type of information would be most helpful?\n" +
                "3. Are they looking for specific details, procedures, contact info, or general guidance?\n" +
                "4. What context or background might be relevant?\n\n" +
                "Respond with a clear, detailed intent analysis that will help search for relevant information.",
                userQuestion
            ));

            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.3);
            generationConfig.put("maxOutputTokens", 200);
            requestBody.put("generationConfig", generationConfig);

            return callGeminiAPI(requestBody);

        } catch (Exception e) {
            System.err.println("⚠️ Error in intent analysis: " + e.getMessage());
            return "General inquiry about: " + userQuestion;
        }
    }

    /**
     * Step 3: Search PDF content using Gemini for relevant sections
     */
    private String searchPdfContentWithGemini(String userQuestion, String userIntent) {
        try {
            // Get all available PDF content (Info.txt and any PDFs)
            String allPdfContent = getAllAvailableContent();

            if (allPdfContent == null || allPdfContent.trim().isEmpty()) {
                System.out.println("⚠️ No PDF content available for search");
                return null;
            }

            System.out.println("🔍 Using Gemini to search PDF content intelligently");

            JSONObject requestBody = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", String.format(
                "You are an intelligent document searcher. Your task is to find information relevant to the user's question from the provided company documents.\n\n" +
                "User Question: '%s'\n\n" +
                "User Intent Analysis: %s\n\n" +
                "Company Document Content:\n%s\n\n" +
                "Instructions:\n" +
                "1. Carefully read through ALL the document content\n" +
                "2. Find ANY sections that could help answer the user's question\n" +
                "3. Extract relevant policies, procedures, contact information, or facts\n" +
                "4. Include specific details like numbers, dates, names, email addresses\n" +
                "5. If you find relevant information, return it exactly as written\n" +
                "6. If NO relevant information exists, respond with: 'NO_RELEVANT_INFO_FOUND'\n" +
                "7. When in doubt, include potentially useful context\n\n" +
                "Extract all relevant information:",
                userQuestion, userIntent, allPdfContent
            ));

            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.1); // Very low for accuracy
            generationConfig.put("maxOutputTokens", 1000);
            requestBody.put("generationConfig", generationConfig);

            String result = callGeminiAPI(requestBody);

            if (result != null && !result.contains("NO_RELEVANT_INFO_FOUND")) {
                System.out.println("✅ Found relevant content in documents");
                return result;
            } else {
                System.out.println("❌ No relevant content found in documents");
                return null;
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error searching PDF content: " + e.getMessage());
            return null;
        }
    }

    /**
     * Step 4a: Generate optimized response combining PDF content with Gemini understanding
     */
    private String generateOptimizedResponseWithPdfContent(String userQuestion, String pdfContent,
                                                          String conversationContext, String userIntent) {
        try {
            JSONObject requestBody = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", String.format(
                "You are a direct and efficient company assistant for Health Catalyst India. Provide ONLY what the user asked for - no extra information.\n\n" +
                "User Question: '%s'\n\n" +
                "User Intent: %s\n\n" +
                "Conversation Context: %s\n\n" +
                "Relevant Company Information:\n%s\n\n" +
                "RESPONSE GUIDELINES:\n" +
                "1. Give DIRECT answers - no unnecessary explanations\n" +
                "2. If user asks about a person, provide:\n" +
                "   - **Name**: Role\n" +
                "   - **Department**: Team\n" +
                "   - **Manager/Mentor**: If mentioned\n" +
                "   - STOP there - no contact suggestions unless asked\n" +
                "3. For policies, give key points only:\n" +
                "   - **Policy**: Brief description\n" +
                "   - **Key details**: Numbers, dates, requirements\n" +
                "4. Use **bold** for names and important info\n" +
                "5. Use bullet points (•) only when multiple items exist\n" +
                "6. Keep response under 3-4 lines unless complex information\n" +
                "7. NO phrases like 'I understand', 'unfortunately', 'please contact'\n" +
                "8. NO suggestions for further help unless specifically asked\n" +
                "9. If information is not found, simply say 'Information not available in company records'\n\n" +
                "Provide a direct, concise answer:",
                userQuestion, userIntent, conversationContext, pdfContent
            ));

            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.2); // Lower for more direct responses
            generationConfig.put("maxOutputTokens", 512); // Reduced for conciseness
            requestBody.put("generationConfig", generationConfig);

            return callGeminiAPI(requestBody);

        } catch (Exception e) {
            System.err.println("❌ Error generating optimized response: " + e.getMessage());
            return generateGeminiErrorResponse(userQuestion);
        }
    }

    /**
     * Step 4b: Handle with Gemini API when no relevant PDF information exists
     */
    private String generateGeminiStandaloneResponse(String userQuestion, String conversationContext) {
        try {
            JSONObject requestBody = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();

            JSONObject part = new JSONObject();
            part.put("text", String.format(
                "You are a direct company assistant for Health Catalyst India. The user's question couldn't be answered from company documents.\n\n" +
                "User Question: '%s'\n\n" +
                "Conversation Context: %s\n\n" +
                "RESPONSE GUIDELINES:\n" +
                "1. Be direct and concise - no lengthy explanations\n" +
                "2. Simply state: 'Information not available in company records'\n" +
                "3. If it's a general question you can answer, provide brief helpful info\n" +
                "4. Use **bold** for important information\n" +
                "5. Keep response under 2-3 lines\n" +
                "6. NO phrases like 'unfortunately', 'I apologize', 'please contact'\n" +
                "7. Only suggest HR contact if specifically about policies/employees\n\n" +
                "Generate a brief, direct response:",
                userQuestion, conversationContext
            ));

            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.3);
            generationConfig.put("maxOutputTokens", 256); // Very short for no-info responses
            requestBody.put("generationConfig", generationConfig);

            return callGeminiAPI(requestBody);

        } catch (Exception e) {
            System.err.println("❌ Error generating standalone response: " + e.getMessage());
            return generateGeminiErrorResponse(userQuestion);
        }
    }

    /**
     * Get all available content from Info.txt and PDF files
     */
    private String getAllAvailableContent() {
        try {
            StringBuilder allContent = new StringBuilder();

            // Get Info.txt content first (primary source)
            String infoContent = documentService.getDocumentContent("Info.txt");
            if (infoContent != null && !infoContent.trim().isEmpty()) {
                allContent.append("=== COMPANY INFORMATION SUMMARY ===\n");
                allContent.append(infoContent).append("\n\n");
            }

            // Get all other PDF content
            Map<String, String> pdfContents = documentService.getAllDocumentContents();
            for (Map.Entry<String, String> entry : pdfContents.entrySet()) {
                if (!entry.getKey().equals("Info.txt") && entry.getValue() != null) {
                    allContent.append("=== ").append(entry.getKey()).append(" ===\n");
                    allContent.append(entry.getValue()).append("\n\n");
                }
            }

            return allContent.toString();

        } catch (Exception e) {
            System.err.println("⚠️ Error getting all available content: " + e.getMessage());
            return null;
        }
    }

    /**
     * Core method to call Gemini API
     */
    private String callGeminiAPI(JSONObject requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String url = GEMINI_API_URL + "?key=" + geminiApiKey;
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode candidates = jsonResponse.path("candidates");
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode content = firstCandidate.path("content");
                    JsonNode parts = content.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).path("text").asText();
                    }
                }
                return "I apologize, but I couldn't generate a proper response.";
            } else {
                System.err.println("❌ Gemini API error: " + response.getStatusCode());
                return "I apologize, but I'm having trouble accessing the AI service right now.";
            }

        } catch (Exception e) {
            System.err.println("❌ Error calling Gemini API: " + e.getMessage());
            return "I'm experiencing technical difficulties. Please try again later.";
        }
    }

    private String generateGeminiErrorResponse(String userQuestion) {
        return "I apologize, but I'm experiencing technical difficulties at the moment. " +
               "Please try again in a few minutes or contact HR directly for assistance with your question: \"" +
               userQuestion + "\"";
    }

    private UserIntent parseIntentResponse(String response) {
        try {
            String[] parts = response.split("\\|");
            if (parts.length >= 3) {
                String category = parts[0].trim();
                double confidence = Double.parseDouble(parts[1].trim());
                List<String> topics = Arrays.asList(parts[2].split(","));
                return new UserIntent(category, confidence, topics);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing intent response: " + e.getMessage());
        }
        return new UserIntent("GENERAL", 0.5, Arrays.asList("general"));
    }

    private String extractRelevantSections(String content, String userQuestion, UserIntent intent) {
        // Simple extraction based on keywords - can be enhanced with more sophisticated NLP
        String[] sentences = content.split("\\. ");
        StringBuilder relevant = new StringBuilder();

        for (String sentence : sentences) {
            for (String topic : intent.getKeyTopics()) {
                if (sentence.toLowerCase().contains(topic.toLowerCase()) && relevant.length() < 1000) {
                    relevant.append(sentence).append(". ");
                    break;
                }
            }
        }

        return relevant.length() > 0 ? relevant.toString() : content.substring(0, Math.min(800, content.length()));
    }

    private boolean containsRelevantInfo(String content, String keyword, String userQuestion) {
        return content.toLowerCase().contains(keyword.toLowerCase()) ||
               content.toLowerCase().contains(userQuestion.toLowerCase());
    }

    /**
     * Extract keywords from text for search optimization
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2)
            .filter(word -> !isStopWord(word))
            .collect(Collectors.toList());
    }

    /**
     * Check if a word is a common stop word
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "the", "is", "at", "which", "on", "and", "a", "to", "as", "are",
            "was", "for", "an", "be", "by", "this", "that", "it", "with",
            "from", "they", "we", "been", "have", "has", "had", "were", "what", "how"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * Inner class for User Intent
     */
    public static class UserIntent {
        private String category;
        private double confidence;
        private List<String> keyTopics;

        public UserIntent(String category, double confidence, List<String> keyTopics) {
            this.category = category;
            this.confidence = confidence;
            this.keyTopics = keyTopics;
        }

        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public List<String> getKeyTopics() { return keyTopics; }
    }
}
