package com.example.civic_issue.Controller;

import com.example.civic_issue.Service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");

        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Add country code
        }
        System.out.println("Sending OTP to: " + phoneNumber);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("Phone number is required");
        }

        otpService.generateAndSendOtp(phoneNumber);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        String otpCode = body.get("otpCode");
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber; // Add country code
        }
        String token = otpService.verifyOtpAndGetToken(phoneNumber, otpCode);

        if (token != null) {
            // Return JWT token to client
            return ResponseEntity.ok(token);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired OTP");
    }
}
