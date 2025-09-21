package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
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

import java.lang.management.ManagementPermission;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ComplaintRepository complaintRepository;
    // ================== CREATE DEPARTMENT + HEAD ==================
    @PostMapping("/create")
    public ResponseEntity<?> createDepartment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DepartmentRequest request) {

        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body(new SimpleResponse(false, "Access denied"));
        }

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

            // Link head to department
            department.setHead(head);
            departmentRepository.save(department);
        }

        return ResponseEntity.ok(new SimpleResponse(true, "Department created successfully"));
    }

    // ================== LIST DEPARTMENTS ==================
    @GetMapping("/list")
    public ResponseEntity<?> listDepartments(@RequestHeader("Authorization") String authHeader) {
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body(new SimpleResponse(false, "Access denied"));
        }

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

            // Stats placeholders
            res.setOpenReports(0);
            res.setActiveReports(0);
            res.setResolvedLast30Days(0);
            res.setAvgResolutionTime("N/A");

            return res;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<?> getDepartmentById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long departmentId) {

        String token = authHeader.substring(7);
        String phone = jwtUtil.extractPhoneNumber(token);

        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() == Role.CITIZEN) {
            return ResponseEntity.status(403).body("Access denied");
        }

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

        // Calculate report stats
        int openReports = complaintRepository.findByAssignedTo_Department_Id(departmentId)
                .stream().filter(c -> c.getStatus() == null || c.getStatus() == ComplaintStatus.PENDING)
                .toList().size();

        int activeReports = complaintRepository.findByAssignedTo_Department_Id(departmentId)
                .stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .toList().size();

        int resolvedLast30Days = (int) complaintRepository.findByAssignedTo_Department_Id(departmentId)
                .stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .filter(c -> c.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                .count();

        double avgResolutionDays = complaintRepository.findByAssignedTo_Department_Id(departmentId)
                .stream()
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

    @GetMapping("/data")
    public ResponseEntity<?> getDepartmentData() {
        List<Department> departments = departmentRepository.findAll();

        List<DepartmentWorkloadDTO> workloadDTOs = departments.stream()
                .map(dept -> {
                    long active = complaintRepository.countByAssignedTo_IdAndStatus(dept.getId(), ComplaintStatus.IN_PROGRESS);
                    long pending = complaintRepository.countByAssignedTo_IdAndStatus(dept.getId(), ComplaintStatus.PENDING);
                    long resolved = complaintRepository.countByAssignedTo_IdAndStatus(dept.getId(), ComplaintStatus.RESOLVED);
                    ;

                    return new DepartmentWorkloadDTO(dept.getName(), active, pending, resolved);
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("departmentWorkload", workloadDTOs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/operators")
    public ResponseEntity<?> getDepartmentOperators(@PathVariable Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<User> operators = userRepository.findByDepartmentIdAndRole(id, Role.OPERATOR);

        List<Map<String, Object>> response = operators.stream().map(op -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", op.getId());
            dto.put("name", op.getFullName());
            dto.put("status", "available"); // later replace with real logic
            dto.put("workload", op.getAssignedComplaints() != null ? op.getAssignedComplaints().size() : 0);
            dto.put("email", op.getEmail());
            dto.put("phone", op.getPhoneNumber());
            dto.put("department", department.getName());
            dto.put("joinDate", op.getCreatedAt() != null ? op.getCreatedAt().toLocalDate().toString() : null);
            dto.put("completedReports", 0L); // later calculate via complaintRepository
            dto.put("avgResolutionTime", "N/A"); // replace with real calculation
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
        response.put("status", "available"); // ✅ placeholder
        response.put("workload", operator.getAssignedComplaints() != null ? operator.getAssignedComplaints().size() : 0);
        response.put("email", operator.getEmail());
        response.put("phone", operator.getPhoneNumber());
        response.put("department", department != null ? department.getName() : null);
        response.put("joinDate", operator.getCreatedAt() != null ? operator.getCreatedAt().toLocalDate().toString() : null);
        response.put("completedReports", 0L); // ✅ calculate later with complaintRepository
        response.put("avgResolutionTime", "N/A"); // ✅ replace later with real calculation
        response.put("description", operator.getSpecialization() != null
                ? operator.getSpecialization()
                : "No description provided");

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
            dto.put("id", "RPT-op" + operator.getId() + "-" + c.getId()); // frontend expects formatted ID
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

