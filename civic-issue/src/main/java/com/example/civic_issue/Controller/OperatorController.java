package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.OperatorResponse;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import com.example.civic_issue.dto.OperatorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/department/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ✅ Create Operator
    @PostMapping("/create")
    public ResponseEntity<?> createOperator(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody OperatorRequest request) {

        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User head = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (head.getRole() != Role.DEPARTMENT_HEAD) {
            return ResponseEntity.status(403).body(new SimpleResponse(false, "Only department head can create operators"));
        }

        User operator = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.OPERATOR)
                .department(head.getDepartment())
                .createdBy(head)
                .email(request.getEmail())
                .specialization(request.getSpecialization())
                .build();

        userRepository.save(operator);

        return ResponseEntity.ok(new SimpleResponse(true, "Operator created successfully"));
    }

    // ✅ List operators of logged-in department
    @GetMapping("/my-department")
    public ResponseEntity<?> listOperatorsOfMyDepartment(
            @RequestHeader("Authorization") String authHeader) {

        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User head = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (head.getRole() != Role.DEPARTMENT_HEAD || head.getDepartment() == null) {
            return ResponseEntity.status(403).body(new SimpleResponse(false, "Access denied"));
        }

        List<OperatorResponse> response = userRepository.findByRoleAndDepartment_Id(Role.OPERATOR, head.getDepartment().getId())
                .stream()
                .map(user -> new OperatorResponse(user.getId(), user.getPhoneNumber(), head.getDepartment().getName()))
                .toList();

        return ResponseEntity.ok(response);
    }

    // ================== RESPONSE RECORD ==================
    private record SimpleResponse(boolean success, String message) {}
}
