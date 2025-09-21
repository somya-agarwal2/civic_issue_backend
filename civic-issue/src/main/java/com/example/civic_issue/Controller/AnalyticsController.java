package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping("/chart-data")
    public ResponseEntity<?> getAnalyticsChartData() {

        List<Complaint> allComplaints = complaintRepository.findAll();

        // 1️⃣ Report Volume per Month
        Map<String, Long> monthCounts = allComplaints.stream()
                .collect(Collectors.groupingBy(c -> {
                    Month m = c.getCreatedAt().getMonth();
                    return m.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                }, LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> reportVolumeData = monthCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

// 2️⃣ Average response time per department
        List<Map<String, Object>> responseTimeData = departmentRepository.findAll().stream()
                .map(dept -> {
                    List<Complaint> deptComplaints = allComplaints.stream()
                            .filter(c -> c.getAssignedTo() != null && c.getAssignedTo().getDepartment() != null
                                    && c.getAssignedTo().getDepartment().getId().equals(dept.getId()))
                            .collect(Collectors.toList());

                    double avgTime = deptComplaints.stream()
                            .filter(c -> c.getDueDate() != null && c.getCreatedAt() != null)
                            .mapToDouble(c -> (double) java.time.Duration.between(c.getCreatedAt(), c.getDueDate()).toHours() / 24)
                            .average().orElse(0);

                    Map<String, Object> map = new HashMap<>();
                    map.put("name", dept.getName());
                    map.put("time", Math.round(avgTime * 10.0) / 10.0);
                    return map;
                })
                .collect(Collectors.toList());

        // 3️⃣ Issues by category
        Map<String, Long> categoryCounts = allComplaints.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));

        List<String> colors = List.of("#22c55e", "#06b6d4", "#f59e0b", "#ef4444", "#8b5cf6");

        List<Map<String, Object>> issueCategoriesData = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Long> entry : categoryCounts.entrySet()) {
            issueCategoriesData.add(Map.of(
                    "name", entry.getKey(),
                    "value", entry.getValue(),
                    "color", colors.get(i % colors.size())
            ));
            i++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("reportVolumeData", reportVolumeData);
        response.put("responseTimeData", responseTimeData);
        response.put("issueCategoriesData", issueCategoriesData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/report-hotspots")
    public ResponseEntity<?> getReportHotspots() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        Map<String, Long> hotspotCounts = allComplaints.stream()
                .filter(c -> c.getAddress() != null && !c.getAddress().isBlank())
                .collect(Collectors.groupingBy(Complaint::getAddress, Collectors.counting()));

        List<Map<String, Object>> response = hotspotCounts.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", entry.getKey());
                    m.put("reports", entry.getValue());
                    m.put("color", "#10b981");
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getAnalyticsStats() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        long totalReports = allComplaints.size();

        // Average resolution time in days for resolved complaints
        double avgResolutionDays = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED && c.getCreatedAt() != null && c.getDueDate() != null)
                .mapToDouble(c -> java.time.Duration.between(c.getCreatedAt(), c.getDueDate()).toHours() / 24.0)
                .average()
                .orElse(0);

        long reportsInProgress = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS || c.getStatus() == ComplaintStatus.PENDING)
                .count();

        // Departments above target (example: more than 10 resolved complaints in last month)
        long totalDepartments = departmentRepository.count();
        long departmentsAboveTarget = departmentRepository.findAll().stream()
                .filter(dept -> allComplaints.stream()
                        .filter(c -> c.getAssignedTo() != null && c.getAssignedTo().getDepartment() != null
                                && c.getAssignedTo().getDepartment().getId().equals(dept.getId())
                                && c.getStatus() == ComplaintStatus.RESOLVED
                                && c.getCreatedAt().isAfter(LocalDateTime.now().minusMonths(1)))
                        .count() > 10)
                .count();

        Map<String, Object> response = new HashMap<>();
        response.put("totalReports", totalReports);
        response.put("averageResolutionTime", Math.round(avgResolutionDays * 10.0) / 10.0 + " Days");
        response.put("reportsInProgress", reportsInProgress);
        response.put("departmentsAboveTarget", departmentsAboveTarget + " of " + totalDepartments);

        return ResponseEntity.ok(response);
    }

}
