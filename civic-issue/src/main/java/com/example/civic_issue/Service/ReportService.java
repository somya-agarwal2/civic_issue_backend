package com.example.civic_issue.Service;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.repo.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
// In your ReportService.java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {


    @Value("${similarity.service.apiKey}")
    private String apiKey2;

    private final ComplaintRepository complaintRepository;

    public boolean isDuplicate(Complaint newComplaint, Complaint existingComplaint) {
        boolean coordinatesMatch = areCoordinatesClose(newComplaint, existingComplaint);
        boolean textMatch = areTextsSimilar(newComplaint, existingComplaint);
        boolean photoMatch = arePhotosSimilar(newComplaint, existingComplaint);

        return coordinatesMatch && (textMatch || photoMatch);
    }



    public Complaint addOrMergeComplaint(Complaint newComplaint) {
        List<Complaint> existingComplaints = complaintRepository.findAll();
        for (Complaint complaint : existingComplaints) {
            if (isDuplicate(newComplaint, complaint)) {
                // Merge logic here
                // For example, update fields and save
                complaintRepository.save(complaint);
                return complaint;
            }
        }
        Complaint saved = complaintRepository.save(newComplaint);
        return saved;
    }

    private boolean areCoordinatesClose(Complaint c1, Complaint c2) {
        if (c1.getLatitude() == null || c1.getLongitude() == null ||
                c2.getLatitude() == null || c2.getLongitude() == null) return false;
        double distance = haversine(c1.getLatitude(), c1.getLongitude(), c2.getLatitude(), c2.getLongitude());
        return distance < 50; // 50 meters threshold
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Java: Add this method to ReportService
    private boolean areTextsSimilar(Complaint c1, Complaint c2) {
        double similarity = getSemanticSimilarity(c1.getDescription(), c2.getDescription());
        return similarity > 0.7; // threshold for semantic similarity
    }

    private boolean arePhotosSimilar(Complaint c1, Complaint c2) {
        if (c1.getPhotoUrl() == null || c2.getPhotoUrl() == null) return false;
        return c1.getPhotoUrl().equals(c2.getPhotoUrl());
    }


    public double getSemanticSimilarity(String text1, String text2) {
        RestTemplate restTemplate = new RestTemplate();
        String hfUrl = "https://api-inference.huggingface.co/models/sentence-transformers/all-MiniLM-L6-v2";
        String apiKey = apiKey2; // Store securely

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> payload = Map.of("inputs", List.of(text1, text2));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(hfUrl, request, Map.class);
        // Parse similarity from response (depends on model output)
        // Example: double similarity = (Double) response.getBody().get("similarity");
        // Adjust parsing as per actual response structure
        return (Double) response.getBody().get("similarity");}


}

