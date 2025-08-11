package com.project.ChatBot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class EnhancedPdfProcessingService {

    @Autowired
    private IntelligentChatbotService intelligentChatbotService;

    // Cache for processed PDF content
    private final Map<String, ProcessedDocument> documentCache = new HashMap<>();
    private final Map<String, Long> lastProcessedTimes = new HashMap<>();
    private static final long CACHE_TTL = 10 * 60 * 1000; // 10 minutes

    /**
     * Process and extract intelligent content from PDF files
     */
    public String extractIntelligentContent(String filename, String userQuery) {
        try {
            // Check cache first
            if (documentCache.containsKey(filename)) {
                Long lastProcessed = lastProcessedTimes.get(filename);
                if (lastProcessed != null && (System.currentTimeMillis() - lastProcessed) < CACHE_TTL) {
                    ProcessedDocument cachedDoc = documentCache.get(filename);
                    return filterContentByQuery(cachedDoc, userQuery);
                }
            }

            // Process PDF file
            ProcessedDocument processedDoc = processPdfFile(filename);
            if (processedDoc != null) {
                documentCache.put(filename, processedDoc);
                lastProcessedTimes.put(filename, System.currentTimeMillis());
                return filterContentByQuery(processedDoc, userQuery);
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error extracting intelligent content from " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Process a PDF file and extract structured content
     */
    private ProcessedDocument processPdfFile(String filename) {
        try {
            // Try multiple locations for PDF files
            Path pdfPath = findPdfFile(filename);
            if (pdfPath == null || !Files.exists(pdfPath)) {
                System.err.println("PDF file not found: " + filename);
                return null;
            }

            try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String fullText = stripper.getText(document);

                // Process and structure the content
                return structurePdfContent(fullText, filename);

            }

        } catch (IOException e) {
            System.err.println("Error processing PDF " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Find PDF file in multiple possible locations
     */
    private Path findPdfFile(String filename) {
        // Remove .txt extension if present and add .pdf
        String pdfFilename = filename.replace(".txt", ".pdf");

        // Possible locations to search
        String[] searchPaths = {
            "uploaded_docs/" + pdfFilename,
            "src/main/resources/" + pdfFilename,
            pdfFilename
        };

        for (String searchPath : searchPaths) {
            Path path = Paths.get(searchPath);
            if (Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    /**
     * Structure PDF content into searchable sections
     */
    private ProcessedDocument structurePdfContent(String fullText, String filename) {
        ProcessedDocument doc = new ProcessedDocument(filename, fullText);

        // Extract sections based on common patterns
        extractSections(doc, fullText);

        // Extract key-value pairs
        extractKeyValuePairs(doc, fullText);

        // Extract lists and bullet points
        extractLists(doc, fullText);

        // Extract contact information
        extractContactInfo(doc, fullText);

        return doc;
    }

    /**
     * Extract document sections based on headers and patterns
     */
    private void extractSections(ProcessedDocument doc, String text) {
        String[] lines = text.split("\n");
        String currentSection = "";
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            line = line.trim();

            // Detect section headers
            if (isHeaderLine(line)) {
                // Save previous section
                if (!currentSection.isEmpty() && currentContent.length() > 0) {
                    doc.addSection(currentSection, currentContent.toString());
                }

                // Start new section
                currentSection = line;
                currentContent = new StringBuilder();
            } else if (!line.isEmpty()) {
                currentContent.append(line).append("\n");
            }
        }

        // Save last section
        if (!currentSection.isEmpty() && currentContent.length() > 0) {
            doc.addSection(currentSection, currentContent.toString());
        }
    }

    /**
     * Check if a line is a header/section title
     */
    private boolean isHeaderLine(String line) {
        return line.matches("^[A-Z][A-Z\\s]+$") || // ALL CAPS
               line.matches("^\\d+\\.\\s.*") || // Numbered sections
               line.matches("^[A-Z][a-z]+.*:$") || // Title with colon
               line.contains("Policy") || line.contains("Procedure") ||
               line.contains("Process") || line.contains("Guidelines");
    }

    /**
     * Extract key-value pairs from text
     */
    private void extractKeyValuePairs(ProcessedDocument doc, String text) {
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();

            // Look for patterns like "Key: Value" or "Key - Value"
            if (line.contains(":") || line.contains(" - ")) {
                String[] parts = line.split("[:|-]", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    if (!key.isEmpty() && !value.isEmpty() && key.length() < 100) {
                        doc.addKeyValue(key, value);
                    }
                }
            }
        }
    }

    /**
     * Extract lists and bullet points
     */
    private void extractLists(ProcessedDocument doc, String text) {
        String[] lines = text.split("\n");
        List<String> currentList = new ArrayList<>();
        String listTitle = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Check if this line starts a list
            if (line.matches("^[•\\-\\*].*") || line.matches("^\\d+\\..*")) {
                if (currentList.isEmpty() && i > 0) {
                    listTitle = lines[i-1].trim(); // Previous line might be the title
                }
                currentList.add(line);
            } else if (!currentList.isEmpty() && line.isEmpty()) {
                // End of list
                if (!listTitle.isEmpty()) {
                    doc.addList(listTitle, new ArrayList<>(currentList));
                }
                currentList.clear();
                listTitle = "";
            }
        }

        // Save last list
        if (!currentList.isEmpty() && !listTitle.isEmpty()) {
            doc.addList(listTitle, currentList);
        }
    }

    /**
     * Extract contact information
     */
    private void extractContactInfo(ProcessedDocument doc, String text) {
        // Email pattern
        Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        java.util.regex.Matcher emailMatcher = emailPattern.matcher(text);

        while (emailMatcher.find()) {
            doc.addContact("email", emailMatcher.group());
        }

        // Phone pattern
        Pattern phonePattern = Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b|\\b\\(\\d{3}\\)\\s*\\d{3}-\\d{4}\\b");
        java.util.regex.Matcher phoneMatcher = phonePattern.matcher(text);

        while (phoneMatcher.find()) {
            doc.addContact("phone", phoneMatcher.group());
        }
    }

    /**
     * Filter content based on user query using AI
     */
    private String filterContentByQuery(ProcessedDocument doc, String userQuery) {
        StringBuilder relevantContent = new StringBuilder();

        // Add document header
        relevantContent.append("=== ").append(doc.getFilename()).append(" ===\n\n");

        // Use AI to find most relevant sections
        String[] queryWords = userQuery.toLowerCase().split("\\s+");
        Map<String, Double> sectionScores = new HashMap<>();

        // Score each section
        for (Map.Entry<String, String> section : doc.getSections().entrySet()) {
            double score = calculateRelevanceScore(section.getKey() + " " + section.getValue(), queryWords);
            sectionScores.put(section.getKey(), score);
        }

        // Include top scoring sections
        sectionScores.entrySet().stream()
            .filter(entry -> entry.getValue() > 0.1)
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> {
                relevantContent.append("## ").append(entry.getKey()).append("\n");
                relevantContent.append(doc.getSections().get(entry.getKey())).append("\n\n");
            });

        // Add relevant key-value pairs
        for (Map.Entry<String, String> kv : doc.getKeyValues().entrySet()) {
            if (calculateRelevanceScore(kv.getKey() + " " + kv.getValue(), queryWords) > 0.2) {
                relevantContent.append("**").append(kv.getKey()).append(":** ").append(kv.getValue()).append("\n");
            }
        }

        // Add relevant lists
        for (Map.Entry<String, List<String>> list : doc.getLists().entrySet()) {
            String listContent = String.join(" ", list.getValue());
            if (calculateRelevanceScore(list.getKey() + " " + listContent, queryWords) > 0.2) {
                relevantContent.append("\n**").append(list.getKey()).append(":**\n");
                for (String item : list.getValue()) {
                    relevantContent.append(item).append("\n");
                }
                relevantContent.append("\n");
            }
        }

        return relevantContent.toString();
    }

    /**
     * Calculate relevance score for content
     */
    private double calculateRelevanceScore(String content, String[] queryWords) {
        String lowerContent = content.toLowerCase();
        double score = 0.0;

        for (String word : queryWords) {
            if (word.length() > 2) { // Skip very short words
                if (lowerContent.contains(word)) {
                    score += 1.0 / queryWords.length;
                    // Bonus for exact word match
                    if (lowerContent.matches(".*\\b" + word + "\\b.*")) {
                        score += 0.5 / queryWords.length;
                    }
                }
            }
        }

        return Math.min(score, 1.0);
    }

    /**
     * Get all available PDF documents
     */
    public List<String> getAvailablePdfDocuments() {
        List<String> pdfFiles = new ArrayList<>();

        try {
            Path uploadedDocsPath = Paths.get("uploaded_docs");
            if (Files.exists(uploadedDocsPath)) {
                Files.list(uploadedDocsPath)
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .forEach(path -> pdfFiles.add(path.getFileName().toString()));
            }
        } catch (Exception e) {
            System.err.println("Error listing PDF documents: " + e.getMessage());
        }

        return pdfFiles;
    }

    // Inner class for processed document structure
    public static class ProcessedDocument {
        private String filename;
        private String fullText;
        private Map<String, String> sections = new HashMap<>();
        private Map<String, String> keyValues = new HashMap<>();
        private Map<String, List<String>> lists = new HashMap<>();
        private Map<String, String> contacts = new HashMap<>();

        public ProcessedDocument(String filename, String fullText) {
            this.filename = filename;
            this.fullText = fullText;
        }

        public void addSection(String title, String content) {
            sections.put(title, content);
        }

        public void addKeyValue(String key, String value) {
            keyValues.put(key, value);
        }

        public void addList(String title, List<String> items) {
            lists.put(title, items);
        }

        public void addContact(String type, String contact) {
            contacts.put(type, contact);
        }

        // Getters
        public String getFilename() { return filename; }
        public String getFullText() { return fullText; }
        public Map<String, String> getSections() { return sections; }
        public Map<String, String> getKeyValues() { return keyValues; }
        public Map<String, List<String>> getLists() { return lists; }
        public Map<String, String> getContacts() { return contacts; }
    }
}
