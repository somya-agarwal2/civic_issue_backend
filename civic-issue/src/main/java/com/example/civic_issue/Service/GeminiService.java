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
            // 1️⃣ Build request payload
            String requestJson = buildPrompt(title, description, photoUrl);

            // 2️⃣ Gemini API endpoint
            String url = "https://vertexai.googleapis.com/v1/models/gemini:predict?key=" + apiKey;

            // 3️⃣ HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // 4️⃣ Execute POST request
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 5️⃣ Parse the API response
            return parseResponse(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            // fallback to LOW if anything fails
            return Priority.LOW;
        }
    }

    private String buildPrompt(String title, String description, String photoUrl) {
        String text = title + " " + description;
        if (photoUrl != null && !photoUrl.isBlank()) {
            text += " Photo URL: " + photoUrl;
        }

        JSONObject instance = new JSONObject();
        instance.put("prompt", text);
        instance.put("parameters", new JSONObject().put("temperature", 0.0));

        JSONArray instances = new JSONArray();
        instances.put(instance);

        JSONObject json = new JSONObject();
        json.put("instances", instances);

        return json.toString();
    }

    private Priority parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return Priority.LOW;

        try {
            JSONObject json = new JSONObject(responseBody);
            JSONArray predictions = json.optJSONArray("predictions");

            if (predictions != null && predictions.length() > 0) {
                String priorityText = predictions.getString(0).trim().toUpperCase();

                switch (priorityText) {
                    case "HIGH": return Priority.HIGH;
                    case "MEDIUM": return Priority.MEDIUM;
                    default: return Priority.LOW;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Default fallback
        return Priority.LOW;
    }
}
