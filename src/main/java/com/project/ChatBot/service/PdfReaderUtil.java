package com.project.ChatBot.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class PdfReaderUtil {
    public static String extractText(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return new PDFTextStripper().getText(document);
        } catch (Exception e) {
            System.out.println("Error reading PDF: " + e.getMessage());
            return "";
        }
    }

    public static String extractTextFromPdf(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            System.out.println("Successfully extracted text from PDF: " + filePath + " (length: " + text.length() + ")");
            return text;
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF " + filePath + ": " + e.getMessage());
            return "";
        }
    }

    public static boolean isPdfReadable(String filePath) {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            return document.getNumberOfPages() > 0;
        } catch (Exception e) {
            System.err.println("PDF not readable: " + filePath + " - " + e.getMessage());
            return false;
        }
    }
}
