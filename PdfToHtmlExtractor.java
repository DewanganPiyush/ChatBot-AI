import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PdfToHtmlExtractor {

    private static final String UPLOADED_DOCS_PATH = "uploaded_docs";
    private static final String OUTPUT_HTML_PATH = "extracted_text.html";

    public static void main(String[] args) {
        try {
            StringBuilder allText = new StringBuilder();

            // Get all PDF files from uploaded_docs folder
            File folder = new File(UPLOADED_DOCS_PATH);
            if (!folder.exists() || !folder.isDirectory()) {
                System.err.println("uploaded_docs folder not found");
                return;
            }

            File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

            if (pdfFiles == null || pdfFiles.length == 0) {
                System.out.println("No PDF files found in uploaded_docs folder");
                return;
            }

            System.out.println("Found " + pdfFiles.length + " PDF file(s) to process");

            // Extract text from each PDF
            for (File pdfFile : pdfFiles) {
                System.out.println("Processing: " + pdfFile.getName());
                String text = extractTextFromPdf(pdfFile.getAbsolutePath());
                if (text != null && !text.trim().isEmpty()) {
                    // Clean up the text - remove extra newlines and normalize spacing
                    String cleanedText = text.replaceAll("\\s+", " ").trim();
                    if (allText.length() > 0 && !cleanedText.isEmpty()) {
                        allText.append(" ");
                    }
                    allText.append(cleanedText);
                }
            }

            // Create HTML file
            createHtmlFile(allText.toString());
            System.out.println("HTML file created successfully: " + OUTPUT_HTML_PATH);
            System.out.println("Total characters extracted: " + allText.length());

        } catch (Exception e) {
            System.err.println("Error processing PDFs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String extractTextFromPdf(String pdfPath) {
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            System.out.println("Successfully extracted text from: " + pdfPath);
            return text;
        } catch (IOException e) {
            System.err.println("Error extracting text from PDF " + pdfPath + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void createHtmlFile(String text) throws IOException {
        try (FileWriter writer = new FileWriter(OUTPUT_HTML_PATH)) {
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html>\n");
            writer.write("<head>\n");
            writer.write("<title>Extracted PDF Text</title>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("<p>" + escapeHtml(text) + "</p>\n");
            writer.write("</body>\n");
            writer.write("</html>");
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}
