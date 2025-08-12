package com.project.ChatBot.controller;

import com.project.ChatBot.service.PdfToHtmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PdfToHtmlController {

    @Autowired
    private PdfToHtmlService pdfToHtmlService;

    @GetMapping("/extract-pdf-to-html")
    public String extractPdfToHtml() {
        try {
            pdfToHtmlService.extractAllPdfsToHtml();
            return "PDF text extraction completed successfully. Check extracted_text.html file.";
        } catch (Exception e) {
            return "Error during PDF text extraction: " + e.getMessage();
        }
    }
}
