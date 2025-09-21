package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class DepartmentReportController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<?> getDepartmentReports(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long departmentId) {

        String token = authHeader.substring(7);
        String phoneNumber = jwtUtil.extractPhoneNumber(token);

        User currentUser = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only admin/department_head/operator roles allowed
        if (!(currentUser.getRole() == Role.SUPER_ADMIN
                || currentUser.getRole() == Role.DEPARTMENT_HEAD
                || currentUser.getRole() == Role.OPERATOR)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        // Filter complaints by department
        List<Complaint> complaints;
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            complaints = complaintRepository.findByAssignedTo_Department_Id(departmentId);
        } else {
            // department_head/operator can only see their own department
            if (currentUser.getDepartment() == null || !currentUser.getDepartment().getId().equals(departmentId)) {
                return ResponseEntity.status(403).body("Access denied for this department");
            }
            complaints = complaintRepository.findByAssignedTo_Department_Id(departmentId);
        }

        // Map to frontend-friendly response
        List<Object> response = complaints.stream().map(c -> {
            return new Object() {
                public final Long id = c.getId();
                public final String title = c.getTitle();
                public final String description = c.getDescription();
                public final String dateReported = c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : null;
                public final String department = c.getAssignedTo() != null && c.getAssignedTo().getDepartment() != null
                        ? c.getAssignedTo().getDepartment().getName()
                        : null;
                public final String priority = c.getPriority() != null ? c.getPriority().name().toLowerCase() : null;
                public final String status = c.getStatus() != null ? c.getStatus().name().toLowerCase() : null;
                public final String assignedTo = c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : null;
            };
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
