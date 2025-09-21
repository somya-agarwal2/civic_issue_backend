package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.DepartmentRequest;
import com.example.civic_issue.dto.DepartmentResponse;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.DepartmentRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ✅ Create Department + Optional Head
    @PostMapping("/create")
    public ResponseEntity<?> createDepartment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DepartmentRequest request) {

        // Check Super Admin
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        // 1️⃣ Create Department
        Department department = Department.builder()
                .name(request.getDepartmentName())
                .build();
        departmentRepository.save(department);

        // 2️⃣ Optional: create department head
        if (request.getPhoneNumber() != null && request.getPassword() != null) {
            User head = User.builder()
                    .phoneNumber(request.getPhoneNumber())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.DEPARTMENT_HEAD)
                    .department(department)
                    .build();
            userRepository.save(head);
        }

        return ResponseEntity.ok("Department created successfully");
    }

    // ✅ List all Departments
    @GetMapping("/list")
    public ResponseEntity<List<DepartmentResponse>> listDepartments(
            @RequestHeader("Authorization") String authHeader) {

        // ✅ 1. Authenticate Super Admin
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).build();
        }

        // ✅ 2. Fetch departments
        List<DepartmentResponse> response = departmentRepository.findAll().stream()
                .map(dept -> new DepartmentResponse(
                        dept.getId(),
                        dept.getName(),
                        dept.getHead() != null ? dept.getHead().getPhoneNumber() : null,
                        dept.getOperators() != null ? dept.getOperators().size() : 0
                ))
                .collect(Collectors.toList());

        // ✅ 3. Return clean JSON
        return ResponseEntity.ok(response);
    }

}
