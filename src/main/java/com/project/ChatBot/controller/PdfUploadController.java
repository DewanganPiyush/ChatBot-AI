package com.project.ChatBot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@CrossOrigin
public class PdfUploadController {

    private final String UPLOAD_DIR = "uploaded_docs";

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            // Create the directory if it doesn't exist
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("Directory created: " + created);
            }

            // Check original file name
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            // Create destination file
            File destFile = new File(dir, originalFilename);
            file.transferTo(destFile);

            System.out.println("PDF saved to: " + destFile.getAbsolutePath());

            return ResponseEntity.ok("Uploaded successfully: " + originalFilename);
        } catch (Exception e) {
            e.printStackTrace(); // log full stack trace
            return ResponseEntity.status(500).body("Upload failed: " + e.getMessage());
        }
    }
}
