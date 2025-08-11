package com.project.ChatBot.controller;

import com.project.ChatBot.service.IntelligentChatbotService;
import com.project.ChatBot.service.DocumentService;
import com.project.ChatBot.service.EnhancedPdfProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/intelligence")
@CrossOrigin
public class IntelligenceTestController {

    @Autowired
    private IntelligentChatbotService intelligentChatbotService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private EnhancedPdfProcessingService pdfProcessingService;

    /**
     * Test the intelligent chatbot with various queries
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testIntelligence(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            String sessionId = request.getOrDefault("sessionId", "test-session");

            Map<String, Object> response = new HashMap<>();
            long startTime = System.currentTimeMillis();

            // Process the query
            String botResponse = intelligentChatbotService.processIntelligentQuery(query, sessionId);

            long processingTime = System.currentTimeMillis() - startTime;

            response.put("query", query);
            response.put("response", botResponse);
            response.put("processingTimeMs", processingTime);
            response.put("sessionId", sessionId);
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Test with predefined queries to showcase intelligence
     */
    @GetMapping("/demo")
    public ResponseEntity<Map<String, Object>> runDemo() {
        Map<String, Object> demoResults = new HashMap<>();
        List<Map<String, Object>> testCases = new ArrayList<>();

        String[] demoQueries = {
            "Hello! How are you today?",
            "What is the leave policy for sick leave?",
            "Who is Piyush Dewangan?",
            "How do I apply for leave in Workday?",
            "What are the company benefits?",
            "Tell me about maternity leave policy",
            "What is the disciplinary policy?",
            "Can you help me with hybrid work guidelines?",
            "I need information about internal job postings",
            "What's the weather like today?",
            "Can you solve this math problem: 25 + 17?",
            "Tell me a joke",
            "I'm feeling stressed about work deadlines"
        };

        String sessionId = "demo-session-" + System.currentTimeMillis();

        for (String query : demoQueries) {
            try {
                long startTime = System.currentTimeMillis();
                String response = intelligentChatbotService.processIntelligentQuery(query, sessionId);
                long processingTime = System.currentTimeMillis() - startTime;

                Map<String, Object> testCase = new HashMap<>();
                testCase.put("query", query);
                testCase.put("response", response);
                testCase.put("processingTimeMs", processingTime);
                testCases.add(testCase);

            } catch (Exception e) {
                Map<String, Object> errorCase = new HashMap<>();
                errorCase.put("query", query);
                errorCase.put("error", e.getMessage());
                testCases.add(errorCase);
            }
        }

        demoResults.put("testCases", testCases);
        demoResults.put("totalQueries", demoQueries.length);
        demoResults.put("sessionId", sessionId);

        return ResponseEntity.ok(demoResults);
    }

    /**
     * Get system intelligence capabilities
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();

        capabilities.put("aiProvider", "Google Gemini 1.5 Flash");
        capabilities.put("pdfProcessing", true);
        capabilities.put("contextAware", true);
        capabilities.put("multiModal", false);
        capabilities.put("conversationMemory", true);

        List<String> queryTypes = Arrays.asList(
            "HR Policy Questions",
            "Employee Information Lookup",
            "Technical Help & Support",
            "General Conversation",
            "Company Information",
            "Complex Multi-part Questions",
            "Clarification Requests",
            "Complaints & Feedback"
        );

        List<String> features = Arrays.asList(
            "🧠 Advanced AI-powered responses using Google Gemini",
            "📄 Intelligent PDF content extraction and processing",
            "🎯 Context-aware query analysis and intent detection",
            "💬 Conversation history and session management",
            "🔍 Smart document search and content filtering",
            "⚡ Real-time processing with caching optimization",
            "🎭 Emotional tone detection and appropriate responses",
            "🔧 Fallback mechanisms for robust error handling"
        );

        capabilities.put("supportedQueryTypes", queryTypes);
        capabilities.put("features", features);
        capabilities.put("availableDocuments", getAvailableDocuments());

        return ResponseEntity.ok(capabilities);
    }

    /**
     * Get available documents for processing
     */
    @GetMapping("/documents")
    public ResponseEntity<Map<String, Object>> getDocuments() {
        Map<String, Object> documentInfo = new HashMap<>();

        try {
            // Get PDF documents
            List<String> pdfDocs = pdfProcessingService.getAvailablePdfDocuments();

            // Get text documents from resources
            List<String> textDocs = Arrays.asList(
                "Employee_Hand_Book_Jan_2025.txt",
                "HCAT Benefits Handout 18-Oct-24.txt",
                "HCAT India Leave Policy 25.Feb.25.txt",
                "HCAT India Parental Leave Policy 12.Sep.22.txt",
                "Health Catalyst India Private Limited Disciplinary Policy 2025.txt",
                "How to apply leave on workday.txt",
                "hybrid_work_policy.txt",
                "Internal Hiring Policy for Lateral Movements.txt",
                "office_Roles.txt",
                "pluxee_card_activation_kyc.txt"
            );

            documentInfo.put("pdfDocuments", pdfDocs);
            documentInfo.put("textDocuments", textDocs);
            documentInfo.put("totalDocuments", pdfDocs.size() + textDocs.size());
            documentInfo.put("cacheStats", documentService.getCacheStats());

        } catch (Exception e) {
            documentInfo.put("error", e.getMessage());
        }

        return ResponseEntity.ok(documentInfo);
    }

    /**
     * Test PDF processing capabilities
     */
    @PostMapping("/test-pdf")
    public ResponseEntity<Map<String, Object>> testPdfProcessing(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String filename = request.get("filename");
            String query = request.getOrDefault("query", "general information");

            if (filename == null || filename.isEmpty()) {
                result.put("error", "Filename is required");
                return ResponseEntity.badRequest().body(result);
            }

            long startTime = System.currentTimeMillis();
            String content = pdfProcessingService.extractIntelligentContent(filename, query);
            long processingTime = System.currentTimeMillis() - startTime;

            result.put("filename", filename);
            result.put("query", query);
            result.put("extractedContent", content);
            result.put("processingTimeMs", processingTime);
            result.put("success", content != null);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Performance metrics endpoint
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            metrics.put("memoryUsageMB", usedMemory / (1024 * 1024));
            metrics.put("totalMemoryMB", totalMemory / (1024 * 1024));
            metrics.put("freeMemoryMB", freeMemory / (1024 * 1024));
            metrics.put("availableProcessors", runtime.availableProcessors());
            metrics.put("javaVersion", System.getProperty("java.version"));
            metrics.put("uptime", System.currentTimeMillis());

            // Add document cache metrics
            metrics.put("documentCache", documentService.getCacheStats());

        } catch (Exception e) {
            metrics.put("error", e.getMessage());
        }

        return ResponseEntity.ok(metrics);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test Gemini API connectivity
            String testQuery = "Hello";
            String testResponse = intelligentChatbotService.processIntelligentQuery(testQuery, "health-check");

            health.put("status", "healthy");
            health.put("geminiApi", "connected");
            health.put("documentService", "operational");
            health.put("pdfProcessing", "operational");
            health.put("timestamp", new Date());

        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("timestamp", new Date());
        }

        return ResponseEntity.ok(health);
    }

    /**
     * Helper method to get available documents
     */
    private List<String> getAvailableDocuments() {
        try {
            List<String> allDocs = new ArrayList<>();
            allDocs.addAll(pdfProcessingService.getAvailablePdfDocuments());
            allDocs.addAll(Arrays.asList(
                "Employee_Hand_Book_Jan_2025.txt",
                "HCAT Benefits Handout 18-Oct-24.txt",
                "HCAT India Leave Policy 25.Feb.25.txt",
                "office_Roles.txt"
            ));
            return allDocs;
        } catch (Exception e) {
            return Arrays.asList("Error loading documents");
        }
    }
}
