package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // GET user-specific settings
    @GetMapping
    public ResponseEntity<?> getSettings(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User currentUser = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = Map.of(
                    "profile", Map.of(
                            "fullName", currentUser.getFullName(),
                            "jobTitle", currentUser.getRole().name(),
                            "contactEmail", currentUser.getEmail()
                    ),
                    "preferences", Map.of(
                            "darkMode", false,
                            "clock24h", false,
                            "language", "English (US)",
                            "timezone", "UTC (Coordinated Universal Time)"
                    ),
                    "notifications", Map.of(
                            "newReportEmail", true,
                            "statusUpdateEmail", true,
                            "deadlineReminders", true
                    ),
                    "system", Map.of(
                            "userManagement", currentUser.getRole() == null ? false : currentUser.getRole().name().contains("ADMIN"),
                            "auditLogs", true,
                            "defaultReportPriority", "Medium"
                    )
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token or user not found"));
        }
    }

    // UPDATE user-specific settings
    @PutMapping
    public ResponseEntity<?> updateSettings(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> updates
    ) {
        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User currentUser = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update profile fields if provided
            Map<String, Object> profile = (Map<String, Object>) updates.get("profile");
            if (profile != null) {
                if (profile.get("fullName") != null) currentUser.setFullName(profile.get("fullName").toString());
                if (profile.get("contactEmail") != null) currentUser.setEmail(profile.get("contactEmail").toString());
            }

            // TODO: update preferences & notifications if stored in DB
            userRepository.save(currentUser);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Settings updated successfully",
                    "updatedSettings", updates
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token or user not found"));
        }
    }
}
