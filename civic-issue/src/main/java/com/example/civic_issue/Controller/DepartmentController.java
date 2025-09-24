package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Service.ComplaintService;
import com.example.civic_issue.dto.DepartmentDetailsResponse;
import com.example.civic_issue.dto.DepartmentRequest;
import com.example.civic_issue.dto.DepartmentResponse;
import com.example.civic_issue.dto.DepartmentWorkloadDTO;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.DepartmentRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ComplaintRepository complaintRepository;
    private final ComplaintService complaintService;

    // ---------------- HELPER ----------------
    private User getAuthorizedUser(String authHeader, Role requiredRole) {
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (requiredRole != null && user.getRole() != requiredRole) {
            throw new RuntimeException("Access denied");
        }
        return user;
    }

    // ================== CREATE DEPARTMENT + HEAD ==================
    @PostMapping("/create")
    public ResponseEntity<?> createDepartment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DepartmentRequest request) {

        getAuthorizedUser(authHeader, Role.SUPER_ADMIN);

        Department department = Department.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .build();
        departmentRepository.save(department);

        // Optional: Create department head
        if (request.getPhone() != null && request.getPassword() != null && request.getDepartmentHead() != null) {
            User head = User.builder()
                    .phoneNumber(request.getPhone())
                    .fullName(request.getDepartmentHead())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.DEPARTMENT_HEAD)
                    .department(department)
                    .build();
            userRepository.save(head);

            department.setHead(head);
            departmentRepository.save(department);
        }

        return ResponseEntity.ok(new SimpleResponse(true, "Department created successfully"));
    }

    // ================== LIST DEPARTMENTS ==================
    @GetMapping("/list")
    public ResponseEntity<?> listDepartments(@RequestHeader("Authorization") String authHeader) {
        getAuthorizedUser(authHeader, Role.SUPER_ADMIN);

        List<DepartmentResponse> response = departmentRepository.findAll().stream().map(dept -> {
            User head = dept.getHead();
            DepartmentResponse res = new DepartmentResponse();
            res.setId(dept.getId());
            res.setName(dept.getName());
            res.setDescription(dept.getDescription());
            res.setEmail(head != null ? head.getEmail() : null);
            res.setPhone(head != null ? head.getPhoneNumber() : null);
            res.setManager(head != null ? head.getFullName() : null);
            res.setAddress(dept.getAddress());

            /// Fetch complaints for this department
            List<Complaint> complaints = complaintRepository.findByDepartment_Id(dept.getId());

            // Calculate average resolution time (in days)
            double avgResolutionDays = complaints.stream()
                    .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED && c.getResolvedAt() != null && c.getCreatedAt() != null)
                    .mapToDouble(c -> Duration.between(c.getCreatedAt(), c.getResolvedAt()).toHours() / 24.0)
                    .average()
                    .orElse(0.0);

            res.setAvgResolutionTime(avgResolutionDays > 0 ? String.format("%.1f days", avgResolutionDays) : "N/A");

            // Stats placeholders (update as needed)
            res.setOpenReports((int) complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.PENDING).count());
            res.setActiveReports((int) complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count());
            res.setResolvedLast30Days((int) complaints.stream()
                    .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                    .filter(c -> c.getResolvedAt() != null && c.getResolvedAt().isAfter(LocalDateTime.now().minusDays(30)))
                    .count());

            return res;
        }).toList();
        return ResponseEntity.ok(response);
    }

    // ================== GET DEPARTMENT BY ID ==================
    @GetMapping("/{departmentId}")
    public ResponseEntity<?> getDepartmentById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long departmentId) {

        getAuthorizedUser(authHeader, null); // allow admins, heads, operators

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        User head = userRepository.findByRoleAndDepartment_Id(Role.DEPARTMENT_HEAD, departmentId)
                .stream().findFirst().orElse(null);

        DepartmentDetailsResponse response = new DepartmentDetailsResponse();
        response.setId(department.getId());
        response.setName(department.getName());
        response.setAddress(department.getAddress());
        response.setDescription(department.getDescription());
        response.setManager(head != null ? head.getFullName() : null);
        response.setEmail(head != null ? head.getEmail() : null);
        response.setPhone(head != null ? head.getPhoneNumber() : null);

        // Correct complaint stats using department reference
        List<Complaint> complaints = complaintRepository.findByDepartment_Id(departmentId);

        int openReports = (int) complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.PENDING)
                .count();

        int activeReports = (int) complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();

        int resolvedLast30Days = (int) complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .filter(c -> c.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .count();

        double avgResolutionDays = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .mapToDouble(c -> Duration.between(c.getCreatedAt(), c.getDueDate() != null ? c.getDueDate() : LocalDateTime.now()).toHours() / 24.0)
                .average()
                .orElse(0.0);

        response.setOpenReports(openReports);
        response.setActiveReports(activeReports);
        response.setResolvedLast30Days(resolvedLast30Days);
        response.setAvgResolutionTime(String.format("%.1f days", avgResolutionDays));

        return ResponseEntity.ok(response);
    }

    // ================== DEPARTMENT WORKLOAD ==================
    @GetMapping("/data")
    public ResponseEntity<?> getDepartmentData() {
        List<Department> departments = departmentRepository.findAll();

        List<DepartmentWorkloadDTO> workloadDTOs = departments.stream()
                .map(dept -> {
                    long active = complaintRepository.countByDepartment_IdAndStatus(dept.getId(), ComplaintStatus.IN_PROGRESS);
                    long pending = complaintRepository.countByDepartment_IdAndStatus(dept.getId(), ComplaintStatus.PENDING);
                    long resolved = complaintRepository.countByDepartment_IdAndStatus(dept.getId(), ComplaintStatus.RESOLVED);

                    return new DepartmentWorkloadDTO(dept.getName(), active, pending, resolved);
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("departmentWorkload", workloadDTOs);

        return ResponseEntity.ok(response);
    }

    // ================== DEPARTMENT OPERATORS ==================
    @GetMapping("/{id}/operators")
    public ResponseEntity<?> getDepartmentOperators(@PathVariable Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<User> operators = userRepository.findByDepartmentIdAndRole(id, Role.OPERATOR);

        List<Map<String, Object>> response = operators.stream().map(op -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", op.getId());
            dto.put("name", op.getFullName());
            dto.put("status", "available");
            dto.put("workload", op.getAssignedComplaints() != null ? op.getAssignedComplaints().size() : 0);
            dto.put("email", op.getEmail());
            dto.put("phone", op.getPhoneNumber());
            dto.put("department", department.getName());
            dto.put("joinDate", op.getCreatedAt() != null ? op.getCreatedAt().toLocalDate().toString() : null);
            dto.put("completedReports", 0L);
            dto.put("avgResolutionTime", "N/A");
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/operators/{operatorId}")
    public ResponseEntity<?> getOperatorById(@PathVariable Long operatorId) {
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        if (operator.getRole() != Role.OPERATOR) {
            return ResponseEntity.badRequest().body(Map.of("error", "User is not an operator"));
        }

        Department department = operator.getDepartment();

        Map<String, Object> response = new HashMap<>();
        response.put("id", operator.getId());
        response.put("name", operator.getFullName());
        response.put("status", "available");
        response.put("workload", operator.getAssignedComplaints() != null ? operator.getAssignedComplaints().size() : 0);
        response.put("email", operator.getEmail());
        response.put("phone", operator.getPhoneNumber());
        response.put("department", department != null ? department.getName() : null);
        response.put("joinDate", operator.getCreatedAt() != null ? operator.getCreatedAt().toLocalDate().toString() : null);
        response.put("completedReports", 0L);
        response.put("avgResolutionTime", "N/A");
        response.put("description", operator.getSpecialization() != null ? operator.getSpecialization() : "No description provided");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/operators/{operatorId}/reports")
    public ResponseEntity<?> getOperatorReports(@PathVariable Long operatorId) {
        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        if (operator.getRole() != Role.OPERATOR) {
            return ResponseEntity.badRequest().body(Map.of("error", "User is not an operator"));
        }

        List<Complaint> complaints = complaintRepository.findByAssignedTo(operator);

        List<Map<String, Object>> response = complaints.stream().map(c -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", "RPT-op" + operator.getId() + "-" + c.getId());
            dto.put("title", c.getTitle());
            dto.put("description", c.getDescription());
            dto.put("priority", c.getPriority() != null ? c.getPriority().name().toLowerCase() : "normal");
            dto.put("status", c.getStatus() != null ? c.getStatus().name().toLowerCase() : "pending");
            dto.put("dateReported", c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : null);
            dto.put("department", operator.getDepartment() != null ? operator.getDepartment().getName() : null);
            dto.put("assignedTo", "op" + operator.getId());
            dto.put("location", c.getAddress());
            return dto;
        }).toList();

        return ResponseEntity.ok(response);
    }


    // ================== RESPONSE RECORD ==================
    private record SimpleResponse(boolean success, String message) {}
}