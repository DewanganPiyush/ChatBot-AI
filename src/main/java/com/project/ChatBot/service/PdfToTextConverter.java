package com.project.ChatBot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PdfToTextConverter {

    private static final String UPLOADED_DOCS_PATH = "uploaded_docs";
    private static final String RESOURCES_PATH = "src/main/resources";

    public static void convertPdfToText(String pdfPath, String txtPath) {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Create parent directories if they don't exist
            Path txtFile = Paths.get(txtPath);
            Files.createDirectories(txtFile.getParent());

            FileWriter writer = new FileWriter(txtPath);
            writer.write(text);
            writer.close();

            System.out.println("PDF text extracted to: " + txtPath);
        } catch (IOException e) {
            System.err.println("Error converting PDF to text: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void convertAllPdfsToText() {
        try {
            File folder = new File(UPLOADED_DOCS_PATH);
            if (!folder.exists() || !folder.isDirectory()) {
                System.err.println("uploaded_docs folder not found or not accessible");
                return;
            }

            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files != null && files.length > 0) {
                System.out.println("Found " + files.length + " PDF files to convert");
                for (File pdfFile : files) {
                    String txtFileName = pdfFile.getName().replace(".pdf", ".txt");
                    String txtPath = RESOURCES_PATH + "/" + txtFileName;
                    convertPdfToText(pdfFile.getAbsolutePath(), txtPath);
                    System.out.println("Converted: " + pdfFile.getName() + " -> " + txtFileName);
                }
                System.out.println("All PDFs converted successfully!");
            } else {
                System.out.println("No PDF files found in uploaded_docs folder");
            }
        } catch (Exception e) {
            System.err.println("Error processing PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract text from PDF and return as String (without saving to file)
     */
    public String extractTextFromPdf(String pdfPath) {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            System.out.println("Successfully extracted text from PDF: " + pdfPath);
            return text;
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF " + pdfPath + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Main method for manual execution
    public static void main(String[] args) {
        convertAllPdfsToText();
    }
}
