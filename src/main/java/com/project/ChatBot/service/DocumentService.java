package com.project.ChatBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    private PdfToTextConverter pdfToTextConverter;

    // Cache for all document contents
    private static final Map<String, String> documentCache = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastReadTimes = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5 minutes cache

    private static final String UPLOADED_DOCS_PATH = "uploaded_docs";

    /**
     * Dynamically discover and get all available document content
     */
    public Map<String, String> getAllDocumentContents() {
        try {
            Map<String, String> allContents = new HashMap<>();
            Path uploadPath = Paths.get(UPLOADED_DOCS_PATH);

            if (!Files.exists(uploadPath)) {
                System.out.println("📁 Upload directory not found: " + UPLOADED_DOCS_PATH);
                return allContents;
            }

            // Get all PDF files in the uploaded_docs directory
            List<Path> pdfFiles = Files.list(uploadPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                .collect(Collectors.toList());

            System.out.println("📄 Found " + pdfFiles.size() + " PDF files to process");

            for (Path pdfFile : pdfFiles) {
                String fileName = pdfFile.getFileName().toString();
                String content = getDocumentContent(fileName);
                if (content != null && !content.trim().isEmpty()) {
                    allContents.put(fileName, content);
                    System.out.println("✅ Processed: " + fileName + " (" + content.length() + " characters)");
                }
            }

            return allContents;

        } catch (Exception e) {
            System.err.println("❌ Error getting all document contents: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get content from a specific document (PDF or text file)
     */
    public String getDocumentContent(String filename) {
        try {
            // Check cache first
            String cacheKey = filename;
            if (documentCache.containsKey(cacheKey)) {
                Long lastReadTime = lastReadTimes.get(cacheKey);
                if (lastReadTime != null && (System.currentTimeMillis() - lastReadTime) < CACHE_TTL) {
                    return documentCache.get(cacheKey);
                }
            }

            String content = null;

            // Try PDF first
            if (filename.toLowerCase().endsWith(".pdf")) {
                content = extractPdfContent(filename);
            } else {
                // Try as text file
                content = readTextFile(filename);
            }

            // Cache the result
            if (content != null) {
                documentCache.put(cacheKey, content);
                lastReadTimes.put(cacheKey, System.currentTimeMillis());
            }

            return content;

        } catch (Exception e) {
            System.err.println("❌ Error getting document content for " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract content from PDF file
     */
    private String extractPdfContent(String filename) {
        try {
            Path pdfPath = Paths.get(UPLOADED_DOCS_PATH, filename);
            if (!Files.exists(pdfPath)) {
                System.out.println("📄 PDF file not found: " + filename);
                return null;
            }

            // Use PdfToTextConverter to extract text directly as String
            String content = pdfToTextConverter.extractTextFromPdf(pdfPath.toString());

            if (content != null && !content.trim().isEmpty()) {
                System.out.println("📄 Successfully extracted content from PDF: " + filename);
                return content;
            } else {
                System.out.println("⚠️ No content extracted from PDF: " + filename);
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Error extracting PDF content from " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Read content from text file
     */
    private String readTextFile(String filename) {
        try {
            Path textPath = Paths.get(UPLOADED_DOCS_PATH, filename);
            if (!Files.exists(textPath)) {
                System.out.println("📄 Text file not found: " + filename);
                return null;
            }

            return Files.readString(textPath);

        } catch (Exception e) {
            System.err.println("❌ Error reading text file " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Search for relevant content across all documents based on query
     */
    public String searchRelevantContent(String userQuery, String userIntent) {
        try {
            Map<String, String> allDocuments = getAllDocumentContents();
            StringBuilder relevantContent = new StringBuilder();

            System.out.println("🔍 Searching through " + allDocuments.size() + " documents for query: " + userQuery);

            for (Map.Entry<String, String> doc : allDocuments.entrySet()) {
                String filename = doc.getKey();
                String content = doc.getValue();

                // Search for relevant sections in this document
                String relevantSections = extractRelevantSections(content, userQuery, userIntent);

                if (relevantSections != null && !relevantSections.trim().isEmpty()) {
                    relevantContent.append("=== ").append(filename).append(" ===\n");
                    relevantContent.append(relevantSections).append("\n\n");
                }
            }

            String result = relevantContent.toString().trim();
            System.out.println("📋 Found " + (result.isEmpty() ? "no" : "relevant") + " content for query");

            return result.isEmpty() ? null : result;

        } catch (Exception e) {
            System.err.println("❌ Error searching relevant content: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract relevant sections from document content based on query
     */
    private String extractRelevantSections(String content, String userQuery, String userIntent) {
        try {
            List<String> queryKeywords = extractKeywords(userQuery);
            List<String> intentKeywords = extractKeywords(userIntent);

            // Combine all keywords
            Set<String> allKeywords = new HashSet<>();
            allKeywords.addAll(queryKeywords);
            allKeywords.addAll(intentKeywords);

            String[] sentences = content.split("\\. ");
            List<ScoredSentence> scoredSentences = new ArrayList<>();

            // Score each sentence based on keyword matches
            for (String sentence : sentences) {
                if (sentence.trim().length() < 10) continue; // Skip very short sentences

                int score = 0;
                String lowerSentence = sentence.toLowerCase();

                for (String keyword : allKeywords) {
                    if (lowerSentence.contains(keyword.toLowerCase())) {
                        score += keyword.length(); // Longer keywords get higher scores
                    }
                }

                if (score > 0) {
                    scoredSentences.add(new ScoredSentence(sentence, score));
                }
            }

            // Sort by score and take top relevant sentences
            scoredSentences.sort((a, b) -> Integer.compare(b.score, a.score));

            StringBuilder result = new StringBuilder();
            int maxSentences = Math.min(10, scoredSentences.size()); // Top 10 sentences
            int totalLength = 0;
            int maxLength = 1500; // Limit total content length

            for (int i = 0; i < maxSentences && totalLength < maxLength; i++) {
                String sentence = scoredSentences.get(i).sentence;
                if (totalLength + sentence.length() <= maxLength) {
                    result.append(sentence).append(". ");
                    totalLength += sentence.length();
                }
            }

            return result.toString().trim();

        } catch (Exception e) {
            System.err.println("❌ Error extracting relevant sections: " + e.getMessage());
            // Return first part of content as fallback
            return content.length() > 1000 ? content.substring(0, 1000) + "..." : content;
        }
    }

    /**
     * Extract keywords from text
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Simple keyword extraction - split by spaces and filter
        return Arrays.stream(text.toLowerCase().split("\\s+"))
            .filter(word -> word.length() > 2) // Skip very short words
            .filter(word -> !isStopWord(word)) // Skip common stop words
            .collect(Collectors.toList());
    }

    /**
     * Check if a word is a common stop word
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "the", "is", "at", "which", "on", "and", "a", "to", "as", "are",
            "was", "for", "an", "be", "by", "this", "that", "it", "with",
            "from", "they", "we", "been", "have", "has", "had", "were"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * Clear document cache
     */
    public void clearCache() {
        documentCache.clear();
        lastReadTimes.clear();
        System.out.println("🗑️ Document cache cleared");
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_documents", documentCache.size());
        stats.put("cache_ttl_minutes", CACHE_TTL / (60 * 1000));
        stats.put("documents_directory", UPLOADED_DOCS_PATH);
        return stats;
    }

    /**
     * Inner class for scoring sentences
     */
    private static class ScoredSentence {
        String sentence;
        int score;

        ScoredSentence(String sentence, int score) {
            this.sentence = sentence;
            this.score = score;
        }
    }

    /**
     * Legacy DocumentInfo class for backward compatibility
     */
    public static class DocumentInfo {
        public String filename;
        public double priority;

        public DocumentInfo(String filename, double priority) {
            this.filename = filename;
            this.priority = priority;
        }
    }
}
