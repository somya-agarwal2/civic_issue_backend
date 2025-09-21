package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.DepartmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/exports/analytics")
@RequiredArgsConstructor
public class AnalyticsExportController {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public ResponseEntity<?> exportAnalyticsData() {
        try {
            // 1️⃣ Prepare analytics data
            List<Complaint> allComplaints = complaintRepository.findAll();
            long totalReports = allComplaints.size();
            long resolvedReports = allComplaints.stream()
                    .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                    .count();

            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("totalReports", totalReports);
            analyticsData.put("resolvedReports", resolvedReports);
            // Add more fields if needed

            // 2️⃣ Write JSON to file
            String date = LocalDate.now().toString();
            String fileName = "civic-flow-analytics-" + date + ".json";
            File file = new File("exports/analytics/" + fileName);
            file.getParentFile().mkdirs(); // create directories if not exist
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, analyticsData);

            // 3️⃣ Return download URL (frontend can call this URL to download)
            String downloadUrl = "/api/exports/analytics/" + fileName;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("downloadUrl", downloadUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadAnalyticsFile(@PathVariable String fileName) {
        try {
            Path filePath = Path.of("exports/analytics/" + fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
