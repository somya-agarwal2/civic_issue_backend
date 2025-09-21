package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.LoginResponse;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.Service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;

    // ================== SEND OTP ==================
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(new SimpleResponse(false, "Phone number is required"));
        }

        phoneNumber = normalizePhoneNumber(phoneNumber);

        System.out.println("Sending OTP to: " + phoneNumber);
        otpService.generateAndSendOtp(phoneNumber);

        return ResponseEntity.ok(new SimpleResponse(true, "OTP sent successfully"));
    }

    // ================== VERIFY OTP ==================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        String otpCode = body.get("otpCode");

        if (phoneNumber == null || phoneNumber.isBlank() || otpCode == null || otpCode.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new SimpleResponse(false, "Phone number and OTP are required"));
        }

        phoneNumber = normalizePhoneNumber(phoneNumber);

        String token = otpService.verifyOtpAndGetToken(phoneNumber, otpCode);

        if (token != null) {
            // Include user info for frontend
            User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);

            LoginResponse.UserInfo userInfo = null;
            if (user != null) {
                userInfo = new LoginResponse.UserInfo(
                        user.getId(),
                        user.getFullName() != null ? user.getFullName() : "Unknown",
                        user.getEmail(),
                        mapRoleToFrontend(user.getRole()),
                        user.getDepartment() != null ? user.getDepartment().getName() : null
                );
            }

            return ResponseEntity.ok(new LoginResponse(userInfo, token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SimpleResponse(false, "Invalid or expired OTP"));
    }

    // ================== HELPER METHODS ==================
    private String mapRoleToFrontend(Role role) {
        if (role == null) return "user";
        return switch (role) {
            case SUPER_ADMIN -> "admin";
            case DEPARTMENT_HEAD -> "department_head";
            case OPERATOR -> "operator";
            default -> "user";
        };
    }

    private String normalizePhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.trim();
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // default country code
        }
        return phoneNumber;
    }

    // ================== RESPONSE RECORD ==================
    record SimpleResponse(boolean success, String message) {}
}
