package com.example.civic_issue.Service;

import com.example.civic_issue.enums.Priority;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Determines the priority of a complaint using Gemini 2.0 Flash API.
     *
     * @param title       Complaint title
     * @param description Complaint description
     * @param photoUrl    Optional photo URL
     * @return Priority enum: LOW, MEDIUM, HIGH
     */
    public Priority determinePriority(String title, String description, String photoUrl) {
        try {
            // Build request payload
            String requestJson = buildPrompt(title, description, photoUrl);

            // Gemini 2.0 Flash API endpoint
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

            // HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Goog-Api-Key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // Execute POST request
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // Parse API response
            return parseResponse(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            // fallback to LOW if anything fails
            return Priority.LOW;
        }
    }

    /**
     * Builds the JSON payload for Gemini 2.0 Flash API with explicit instructions.
     */
    private String buildPrompt(String title, String description, String photoUrl) {
        JSONObject part = new JSONObject();
        part.put("text",
                "Classify this civic complaint strictly into HIGH, MEDIUM, or LOW priority.\n" +
                        "Answer only with the label (HIGH, MEDIUM, or LOW), nothing else.\n\n" +
                        "Title: " + title + "\n" +
                        "Description: " + description +
                        (photoUrl != null && !photoUrl.isBlank() ? "\nPhoto URL: " + photoUrl : "")
        );

        JSONArray parts = new JSONArray();
        parts.put(part);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject json = new JSONObject();
        json.put("contents", contents);

        return json.toString();
    }

    /**
     * Parses the Gemini 2.0 Flash API response to map it to Priority enum.
     */
    private Priority parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return Priority.LOW;

        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray candidates = json.optJSONArray("candidates");

            if (candidates != null && candidates.length() > 0) {
                JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                if (content != null) {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0) {
                        String priorityText = parts.getJSONObject(0).optString("text", "")
                                .trim()
                                .toUpperCase();

                        return switch (priorityText) {
                            case "HIGH" -> Priority.HIGH;
                            case "MEDIUM" -> Priority.MEDIUM;
                            case "LOW" -> Priority.LOW;
                            default -> Priority.LOW;
                        };
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Default fallback
        return Priority.LOW;
    }
}
