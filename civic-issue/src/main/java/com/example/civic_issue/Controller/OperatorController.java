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

    // ✅ Head creates operator
    @PostMapping("/create")
    public ResponseEntity<?> createOperator(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody OperatorRequest request) {

        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User head = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only department head
        if (head.getRole() != Role.DEPARTMENT_HEAD) {
            return ResponseEntity.status(403).body("Only department head can create operators");
        }

        // Create operator
        User operator = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.OPERATOR)
                .department(head.getDepartment()) // operator in same department
                .createdBy(head)
                .build();

        userRepository.save(operator);
        return ResponseEntity.ok("Operator created successfully");
    }

    // ✅ List operators in the department
    @GetMapping("/list")
    public ResponseEntity<?> listOperators(@RequestHeader("Authorization") String authHeader) {
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User head = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (head.getRole() != Role.DEPARTMENT_HEAD) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<User> operators = userRepository.findByRoleAndDepartment_Id(Role.OPERATOR, head.getDepartment().getId());
        return ResponseEntity.ok(operators);
    }

    @GetMapping("/operators/my-department")
    public ResponseEntity<List<OperatorResponse>> listOperatorsOfMyDepartment(
            @RequestHeader("Authorization") String authHeader) {

        // 1️⃣ Extract logged-in user
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User loggedUser = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️⃣ Allow only Department Heads
        if (loggedUser.getRole() != Role.DEPARTMENT_HEAD) {
            return ResponseEntity.status(403).body(null);
        }

        // 3️⃣ Get department
        Department department = loggedUser.getDepartment();
        if (department == null) {
            return ResponseEntity.badRequest().body(null);
        }

        // 4️⃣ Fetch operators in this department
        List<OperatorResponse> response = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.OPERATOR &&
                        user.getDepartment() != null &&
                        user.getDepartment().getId().equals(department.getId()))
                .map(user -> new OperatorResponse(
                        user.getId(),
                        user.getPhoneNumber(),
                        department.getName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

}
