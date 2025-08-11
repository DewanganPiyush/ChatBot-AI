package com.project.ChatBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PdfInitializationService implements ApplicationRunner {

    @Autowired
    private DocumentService documentService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("=== PDF Initialization Service Started ===");

        try {
            convertAllPdfsToTextFiles();
            System.out.println("=== PDF Initialization Service Completed Successfully ===");
        } catch (Exception e) {
            System.err.println("Error during PDF initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void convertAllPdfsToTextFiles() {
        File uploadsFolder = new File("uploaded_docs");

        if (!uploadsFolder.exists() || !uploadsFolder.isDirectory()) {
            System.err.println("uploaded_docs folder not found. Creating it...");
            uploadsFolder.mkdirs();
            return;
        }

        File[] pdfFiles = uploadsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("No PDF files found in uploaded_docs folder");
            return;
        }

        System.out.println("Found " + pdfFiles.length + " PDF files to process:");

        for (File pdfFile : pdfFiles) {
            try {
                String pdfName = pdfFile.getName();
                String txtFileName = pdfName.replace(".pdf", ".txt");
                Path txtPath = Paths.get("src/main/resources/" + txtFileName);

                // Check if text file already exists and is recent
                if (Files.exists(txtPath)) {
                    long txtModified = Files.getLastModifiedTime(txtPath).toMillis();
                    long pdfModified = pdfFile.lastModified();

                    if (txtModified >= pdfModified) {
                        System.out.println("✓ Text file is up to date: " + txtFileName);
                        continue;
                    }
                }

                // Convert PDF to text
                System.out.println("Converting: " + pdfName + " -> " + txtFileName);
                String extractedText = PdfReaderUtil.extractTextFromPdf(pdfFile.getAbsolutePath());

                if (!extractedText.isEmpty()) {
                    // Ensure resources directory exists
                    Files.createDirectories(txtPath.getParent());

                    // Write extracted text to file
                    Files.writeString(txtPath, extractedText);
                    System.out.println("✓ Successfully converted: " + pdfName + " (" + extractedText.length() + " characters)");
                } else {
                    System.err.println("✗ Failed to extract text from: " + pdfName);
                }

            } catch (Exception e) {
                System.err.println("✗ Error processing " + pdfFile.getName() + ": " + e.getMessage());
            }
        }

        // Clear document cache after conversion
        documentService.clearCache();
        System.out.println("Document cache cleared. Ready for user queries.");
    }
}
