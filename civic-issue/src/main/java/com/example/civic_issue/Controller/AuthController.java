package com.example.civic_issue.Controller;

import com.example.civic_issue.Service.AuthService;
import com.example.civic_issue.dto.AuthResponse;
import com.example.civic_issue.dto.ForgotPasswordRequest;
import com.example.civic_issue.dto.LoginRequest;
import com.example.civic_issue.dto.SignupRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        try {

            if (req.password() == null || req.confirmpassword() == null
                    || !req.password().equals(req.confirmpassword())) {
                return ResponseEntity.badRequest().body("Passwords do not match");
            }
            authService.signup(req);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        try {
            if (!req.newPassword().equals(req.confirmPassword())) {
                return ResponseEntity.badRequest().body("Passwords do not match");
            }
            authService.forgotPassword(req);
            return ResponseEntity.ok("Password updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

