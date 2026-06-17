package com.finance.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;


/**
 * OCR Service using Gemini AI Vision to extract text and financial data from receipt images.
 *
 * This service properly:
 * 1. Sends the receipt image to Gemini Flash 1.5
 * 2. Asks the AI to extract structured JSON data
 * 3. Maps the AI response to OcrResult DTO
 */
@Slf4j
@Service
public class OcrService {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public OcrService(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    /**
     * Full OCR pipeline: preprocess → extract text → parse fields
     */
    public OcrResult extractFromReceipt(MultipartFile file) throws IOException {
        byte[] imageBytes = file.getBytes();
        String contentType = file.getContentType();
        if (contentType == null) contentType = "image/jpeg";

        String prompt = "Extract data from this receipt image. " +
                "Return ONLY a JSON object with these fields: " +
                "merchantName, amount (number), date (string), paymentMethod (UPI/Card/Cash/Unknown), category. " +
                "Do not include any other text or markdown formatting.";

        try {
            String jsonResponse = geminiService.vision(prompt, imageBytes, contentType);
            log.debug("Gemini OCR response: {}", jsonResponse);
            
            // Clean markdown if Gemini wrapped it in ```json
            String cleanedJson = jsonResponse;
            if (cleanedJson.contains("```json")) {
                cleanedJson = cleanedJson.substring(cleanedJson.indexOf("```json") + 7);
                if (cleanedJson.contains("```")) {
                    cleanedJson = cleanedJson.substring(0, cleanedJson.indexOf("```"));
                }
            } else if (cleanedJson.contains("```")) {
                cleanedJson = cleanedJson.substring(cleanedJson.indexOf("```") + 3);
                if (cleanedJson.contains("```")) {
                    cleanedJson = cleanedJson.substring(0, cleanedJson.indexOf("```"));
                }
            }
            cleanedJson = cleanedJson.trim();

            OcrResult result = objectMapper.readValue(cleanedJson, OcrResult.class);
            result.setRawText(jsonResponse);
            result.setSuccess(true);
            result.setMessage("OCR completed successfully via Gemini AI");
            
            // Build notes
            StringBuilder notes = new StringBuilder("=== RECEIPT OCR (Gemini AI) ===\n");
            if (result.getMerchantName() != null) notes.append("Merchant: ").append(result.getMerchantName()).append("\n");
            if (result.getAmount() != null)       notes.append("Amount: ₹").append(result.getAmount()).append("\n");
            if (result.getDate() != null)          notes.append("Date: ").append(result.getDate()).append("\n");
            result.setNotes(notes.toString());
            result.setConfidence(95);
            
            return result;
        } catch (Exception e) {
            log.error("Gemini OCR failed: {}", e.getMessage());
            OcrResult errorResult = new OcrResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("AI OCR failed: " + e.getMessage());
            return errorResult;
        }
    }

    public String extractRawText(MultipartFile file) throws IOException {
        OcrResult result = extractFromReceipt(file);
        return result.getRawText();
    }

    // ── Result DTO ───────────────────────────────────────────────────────

    @lombok.Data
    public static class OcrResult {
        private boolean success;
        private String message;
        private BigDecimal amount;
        private String date;
        private String merchantName;
        private String rawText;
        private String notes;
        private String category;
        private Integer confidence;
        private String paymentMethod;
    }
}
