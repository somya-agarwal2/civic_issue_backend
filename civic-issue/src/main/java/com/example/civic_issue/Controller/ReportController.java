package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.ComplaintResponse;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // ---------------------------
    // ✅ Super Admin report
    // ---------------------------
    @PostMapping("/admin")
    public ResponseEntity<List<ComplaintResponse>> getAdminReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReportFilter filter) {

        User admin = getUserFromToken(authHeader);
        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).build();
        }

        List<Complaint> complaints;

        if (filter.getDepartmentId() != null && filter.getStatus() != null) {
            complaints = complaintRepository.findByStatusAndAssignedTo_Department_Id(
                    filter.getStatus(), filter.getDepartmentId());
        } else if (filter.getDepartmentId() != null) {
            complaints = complaintRepository.findByAssignedTo_Department_Id(filter.getDepartmentId());
        } else if (filter.getStatus() != null) {
            complaints = complaintRepository.findByStatus(filter.getStatus());
        } else {
            complaints = complaintRepository.findAll();
        }

        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToComplaintResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ---------------------------
    // ✅ Department Head report
    // ---------------------------
    @PostMapping("/department")
    public ResponseEntity<List<ComplaintResponse>> getDepartmentReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReportFilter filter) {

        User head = getUserFromToken(authHeader);
        if (head.getRole() != Role.DEPARTMENT_HEAD) {
            return ResponseEntity.status(403).build();
        }

        List<Complaint> complaints;
        if (filter.getStatus() != null) {
            complaints = complaintRepository.findByStatusAndAssignedTo_Department_Id(
                    filter.getStatus(), head.getDepartment().getId());
        } else {
            complaints = complaintRepository.findByAssignedTo_Department_Id(head.getDepartment().getId());
        }

        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToComplaintResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ---------------------------
    // ✅ Operator report
    // ---------------------------
    @PostMapping("/operator")
    public ResponseEntity<List<ComplaintResponse>> getOperatorReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReportFilter filter) {

        User operator = getUserFromToken(authHeader);
        if (operator.getRole() != Role.OPERATOR) {
            return ResponseEntity.status(403).build();
        }

        List<Complaint> complaints;
        if (filter.getStatus() != null) {
            complaints = complaintRepository.findByStatusAndAssignedTo_Department_Id(
                    filter.getStatus(), operator.getDepartment().getId());
        } else {
            complaints = complaintRepository.findByAssignedTo_Department_Id(operator.getDepartment().getId());
        }

        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToComplaintResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ---------------------------
    // Helper to extract user from JWT
    // ---------------------------
    private User getUserFromToken(String authHeader) {
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ---------------------------
    // Helper to map Complaint -> ComplaintResponse
    // ---------------------------
    private ComplaintResponse mapToComplaintResponse(Complaint c) {
        return ComplaintResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .category(c.getCategory())
                .address(c.getAddress())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .photoUrl(c.getPhotoUrl())
                .voiceUrl(c.getVoiceUrl())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .priority(c.getPriority() != null ? c.getPriority().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .dueDate(c.getDueDate() != null ? c.getDueDate().toString() : null)
                .assignedToDepartment(
                        c.getAssignedTo() != null && c.getAssignedTo().getDepartment() != null
                                ? c.getAssignedTo().getDepartment().getName()
                                : null
                )
                .build();
    }@GetMapping("/admin")
    public ResponseEntity<?> getAdminReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Optional<ComplaintStatus> status,
            @RequestParam Optional<Long> departmentId) {

        User admin = getUserFromToken(authHeader);
        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<Complaint> complaints;

        if (status.isPresent() && departmentId.isPresent()) {
            complaints = complaintRepository.findByStatusAndAssignedTo_Department_Id(status.get(), departmentId.get());
        } else if (departmentId.isPresent()) {
            complaints = complaintRepository.findByAssignedTo_Department_Id(departmentId.get());
        } else if (status.isPresent()) {
            complaints = complaintRepository.findByStatus(status.get());
        } else {
            complaints = complaintRepository.findAll(); // all complaints
        }

        // Map to DTO
        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/department")
    public ResponseEntity<?> getDepartmentReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Optional<ComplaintStatus> status) {

        User head = getUserFromToken(authHeader);
        if (head.getRole() != Role.DEPARTMENT_HEAD) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<Complaint> complaints = status
                .map(s -> complaintRepository.findByStatusAndAssignedTo_Department_Id(s, head.getDepartment().getId()))
                .orElseGet(() -> complaintRepository.findByAssignedTo_Department_Id(head.getDepartment().getId()));

        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/operator")
    public ResponseEntity<?> getOperatorReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Optional<ComplaintStatus> status) {

        User operator = getUserFromToken(authHeader);
        if (operator.getRole() != Role.OPERATOR) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<Complaint> complaints = status
                .map(s -> complaintRepository.findByStatusAndAssignedTo_Department_Id(s, operator.getDepartment().getId()))
                .orElseGet(() -> complaintRepository.findByAssignedTo_Department_Id(operator.getDepartment().getId()));

        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    // Helper method to map Complaint entity to ComplaintResponse DTO
    private ComplaintResponse mapToDTO(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .address(complaint.getAddress())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .photoUrl(complaint.getPhotoUrl())
                .voiceUrl(complaint.getVoiceUrl())
                .createdAt(complaint.getCreatedAt().toString())
                .priority(complaint.getPriority() != null ? complaint.getPriority().name() : null)
                .status(complaint.getStatus().name())
                .dueDate(complaint.getDueDate() != null ? complaint.getDueDate().toString() : null)
                .assignedToDepartment(complaint.getAssignedTo() != null && complaint.getAssignedTo().getDepartment() != null
                        ? complaint.getAssignedTo().getDepartment().getName()
                        : null)
                .build();
    }

    // ---------------------------
    // DTO for JSON filter
    // ---------------------------
    @Data
    @AllArgsConstructor
    static class ReportFilter {
        private ComplaintStatus status;   // optional
        private Long departmentId;        // optional
    }
}
