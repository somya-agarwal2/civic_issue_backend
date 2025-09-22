package com.example.civic_issue.Service;

import com.example.civic_issue.enums.Priority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Determines the priority of a complaint using Gemini API.
     *
     * @param title       Complaint title
     * @param description Complaint description
     * @param photoUrl    Optional photo URL
     * @return Priority enum: LOW, MEDIUM, HIGH
     */
    public Priority determinePriority(String title, String description, String photoUrl) {
        try {
            // 1️⃣ Build request payload with explicit instructions
            String requestJson = buildPrompt(title, description, photoUrl);

            // 2️⃣ Gemini API endpoint
            String url = "https://vertexai.googleapis.com/v1/models/gemini:predict?key=" + apiKey;

            // 3️⃣ HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // 4️⃣ Execute POST request
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 5️⃣ Parse API response to Priority
            return parseResponse(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            // fallback to LOW if anything fails
            return Priority.LOW;
        }
    }

    /**
     * Builds the JSON payload for Gemini API with clear classification instructions.
     */
    private String buildPrompt(String title, String description, String photoUrl) {
        StringBuilder text = new StringBuilder();
        text.append("You are an AI assistant that classifies civic complaints into HIGH, MEDIUM, or LOW priority.\n");
        text.append("Classify strictly into one of these labels.\n\n");
        text.append("Complaint details:\n");
        text.append("Title: ").append(title).append("\n");
        text.append("Description: ").append(description).append("\n");
        if (photoUrl != null && !photoUrl.isBlank()) {
            text.append("Photo URL: ").append(photoUrl).append("\n");
        }
        text.append("\nAnswer only with HIGH, MEDIUM, or LOW.");

        JSONObject instance = new JSONObject();
        instance.put("content", text.toString());  // Gemini expects 'content' for text

        JSONArray instances = new JSONArray();
        instances.put(instance);

        JSONObject json = new JSONObject();
        json.put("instances", instances);

        return json.toString();
    }

    /**
     * Parses the Gemini API response and maps it to Priority enum.
     */
    private Priority parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return Priority.LOW;

        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray predictions = json.optJSONArray("predictions");

            if (predictions != null && predictions.length() > 0) {
                JSONObject first = predictions.getJSONObject(0);
                String priorityText = first.optString("content", "").trim().toUpperCase();

                return switch (priorityText) {
                    case "HIGH" -> Priority.HIGH;
                    case "MEDIUM" -> Priority.MEDIUM;
                    case "LOW" -> Priority.LOW;
                    default -> Priority.LOW;
                };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Default fallback
        return Priority.LOW;
    }
}
