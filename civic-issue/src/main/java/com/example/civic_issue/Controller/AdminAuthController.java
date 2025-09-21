package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.AdminLoginRequest;
import com.example.civic_issue.dto.LoginResponse;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ================== LOGIN ==================
    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only allow admin-related roles
        if (!(user.getRole() == Role.SUPER_ADMIN ||
                user.getRole() == Role.DEPARTMENT_HEAD ||
                user.getRole() == Role.OPERATOR)) {
            return ResponseEntity.status(403).body(
                    new ErrorResponse("Access denied", false)
            );
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(
                    new ErrorResponse("Invalid password", false)
            );
        }

        // Generate JWT token
        String token = jwtUtil.generateTokenWithRole(user.getPhoneNumber(), user.getRole());

        // Build user info for frontend
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getFullName() != null ? user.getFullName() : "Unknown",
                user.getEmail(),
                mapRoleToFrontend(user.getRole()),
                user.getDepartment() != null ? user.getDepartment().getName() : null
        );

        LoginResponse response = new LoginResponse(userInfo, token);
        return ResponseEntity.ok(response);
    }

    // ================== LOGOUT ==================
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Token deletion handled on frontend
        return ResponseEntity.ok(new SimpleResponse(true, "Logged out successfully"));
    }

    // ================== CURRENT USER ==================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    user.getId(),
                    user.getFullName() != null ? user.getFullName() : "Unknown",
                    user.getEmail(),
                    mapRoleToFrontend(user.getRole()),
                    user.getDepartment() != null ? user.getDepartment().getName() : null
            );

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid token", false));
        }
    }

    // ================== HELPER ==================
    private String mapRoleToFrontend(Role role) {
        return switch (role) {
            case SUPER_ADMIN -> "admin";
            case DEPARTMENT_HEAD -> "department_head";
            case OPERATOR -> "operator";
            default -> "user";
        };
    }

    // ================== RESPONSE RECORDS ==================
    record ErrorResponse(String message, boolean success) {}
    record SimpleResponse(boolean success, String message) {}
}
