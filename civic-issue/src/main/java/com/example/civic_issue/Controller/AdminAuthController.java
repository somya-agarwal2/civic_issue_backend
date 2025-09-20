package com.example.civic_issue.Controller;


import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.AdminLoginRequest;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/admin-login")
    public ResponseEntity<String> adminLogin(@RequestBody AdminLoginRequest request) {

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only allow admin roles
        if (!(user.getRole() == Role.SUPER_ADMIN || user.getRole() == Role.DEPARTMENT_HEAD || user.getRole() == Role.OPERATOR)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid password");
        }

        String token = jwtUtil.generateTokenWithRole(user.getPhoneNumber(), user.getRole());
        return ResponseEntity.ok(token);
    }
}

