package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User currentUser = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Fetch complaints relevant to user
            List<Complaint> complaints = new ArrayList<>();

            if (currentUser.getRole() == Role.CITIZEN) {
                // Citizen: complaints submitted by them
                complaints = complaintRepository.findByUser(currentUser);
            } else if (currentUser.getRole() == Role.OPERATOR || currentUser.getRole() == Role.DEPARTMENT_HEAD) {
                // Operator or Head: complaints assigned to them
                complaints = complaintRepository.findByAssignedTo(currentUser);
            }

            List<Map<String, Object>> notifications = complaints.stream()
                    .sorted(Comparator.comparing(Complaint::getCreatedAt).reversed())
                    .limit(20) // limit recent notifications
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", "N" + c.getId());
                        map.put("type", c.getPriority() != null ? c.getPriority().name().toLowerCase() : "normal");
                        map.put("title", "New " + (c.getPriority() != null ? c.getPriority().name() : "Report") + " Report: " + c.getTitle());
                        map.put("message", c.getDescription());
                        map.put("time", getTimeAgo(c.getCreatedAt()));
                        map.put("status", c.getStatus() != null ? c.getStatus().name().toLowerCase() : "new");
                        map.put("category", "report");
                        return map;
                    }).collect(Collectors.toList());

            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token or user not found"));
        }
    }

    // Helper to get human-readable "time ago"
    private String getTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 60) return minutes + " minutes ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + " hours ago";
        long days = duration.toDays();
        return days + " days ago";
    }
}
