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
    // ✅ Super Admin reports
    // ---------------------------
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminReports(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Optional<ComplaintStatus> status,
            @RequestParam Optional<Long> departmentId) {

        User admin = getUserFromToken(authHeader);
        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<Complaint> complaints = fetchComplaints(status, departmentId);

        List<ComplaintResponse> response = complaints.stream()
                .map(this::mapToDTO)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ---------------------------
    // ✅ Department Head reports
    // ---------------------------
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

    // ---------------------------
    // ✅ Operator reports
    // ---------------------------
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

    // ---------------------------
    // Helper methods
    // ---------------------------
    private User getUserFromToken(String authHeader) {
        String phone = jwtUtil.extractPhoneNumber(authHeader.substring(7));
        return userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private List<Complaint> fetchComplaints(Optional<ComplaintStatus> status, Optional<Long> departmentId) {
        if (status.isPresent() && departmentId.isPresent()) {
            return complaintRepository.findByStatusAndAssignedTo_Department_Id(status.get(), departmentId.get());
        } else if (departmentId.isPresent()) {
            return complaintRepository.findByAssignedTo_Department_Id(departmentId.get());
        } else if (status.isPresent()) {
            return complaintRepository.findByStatus(status.get());
        } else {
            return complaintRepository.findAll();
        }
    }

    private ComplaintResponse mapToDTO(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .departmentId(complaint.getAssignedTo() != null && complaint.getAssignedTo().getDepartment() != null
                        ? complaint.getAssignedTo().getDepartment().getId()
                        : null)

                .address(complaint.getAddress())
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .photoUrl(complaint.getPhotoUrl())
                .voiceUrl(complaint.getVoiceUrl())
                .createdAt(complaint.getCreatedAt().toString())
                .priority(complaint.getPriority() != null ? complaint.getPriority().name() : null)
                .status(complaint.getStatus() != null ? complaint.getStatus().name() : null)
                .dueDate(complaint.getDueDate() != null ? complaint.getDueDate().toString() : null)
                .assignedToDepartment(complaint.getAssignedTo() != null && complaint.getAssignedTo().getDepartment() != null
                        ? complaint.getAssignedTo().getDepartment().getName()
                        : null)
                .build();
    }
}
