package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Service.CloudinaryService;
import com.example.civic_issue.Service.DepartmentAssignmentService;
import com.example.civic_issue.Service.GeminiService;
import com.example.civic_issue.Service.LocationService;
import com.example.civic_issue.dto.ComplaintRequest;
import com.example.civic_issue.dto.ComplaintResponse;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.DepartmentRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final CloudinaryService cloudinaryService;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final GeminiService geminiService;
    private final DepartmentAssignmentService departmentAssignmentService;
    private final LocationService locationService;
    private final DepartmentRepository departmentRepository;


    // ================== CREATE COMPLAINT ==================
    @PostMapping(
            value = "/create",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createComplaintMultipart(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam(value = "photo", required = false) MultipartFile photoFile,
            @RequestParam(value = "voice", required = false) MultipartFile voiceFile
    ) {
        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User citizen = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("Citizen not found"));

            if (citizen.getRole() != Role.CITIZEN) {
                return ResponseEntity.status(403).body(new SimpleResponse(false, "Only citizens can file complaints"));
            }

            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            // Upload files to Cloudinary
            String photoUrl = null;
            if (photoFile != null && !photoFile.isEmpty()) {
                photoUrl = cloudinaryService.uploadMultipartFile(photoFile, "complaints/photos");
            }

            String voiceUrl = null;
            if (voiceFile != null && !voiceFile.isEmpty()) {
                voiceUrl = cloudinaryService.uploadMultipartFile(voiceFile, "complaints/voices");
            }

            // Determine priority with AI service
            Priority priority = geminiService.determinePriority(
                    title,
                    description,
                    photoUrl
            );

            LocalDateTime dueDate = switch (priority) {
                case HIGH -> LocalDateTime.now().plusDays(3);
                case MEDIUM -> LocalDateTime.now().plusDays(7);
                case LOW -> LocalDateTime.now().plusMonths(3);
            };

            String fetchedAddress = locationService.getAddressFromCoordinates(
                    citizen.getLatitude(),
                    citizen.getLongitude()
            );

            Complaint complaint = Complaint.builder()
                    .title(title)
                    .description(description)
                    .department(department)
                    .address(fetchedAddress)
                    .latitude(citizen.getLatitude())
                    .longitude(citizen.getLongitude())
                    .photoUrl(photoUrl)
                    .voiceUrl(voiceUrl)
                    .priority(priority)
                    .status(ComplaintStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .dueDate(dueDate)
                    .user(citizen)
                    .build();

            // Assign department head if exists
            User departmentHead = departmentAssignmentService.getDepartmentHeadForDepartment(department);
            if (departmentHead != null) {
                complaint.setAssignedTo(departmentHead);
                complaint.setAssignedAt(LocalDateTime.now());
            }

            Complaint saved = complaintRepository.save(complaint);

            // Build timeline
            List<ComplaintResponse.TimelineEvent> timeline = new ArrayList<>();
            timeline.add(new ComplaintResponse.TimelineEvent(
                    saved.getCreatedAt().toString(),
                    "Report submitted",
                    citizen.getFullName()
            ));
            if (saved.getAssignedTo() != null) {
                timeline.add(new ComplaintResponse.TimelineEvent(
                        saved.getAssignedAt().toString(),
                        "Report assigned",
                        saved.getAssignedTo().getFullName()
                ));
            }

            ComplaintResponse response = ComplaintResponse.builder()
                    .id(saved.getId())
                    .title(saved.getTitle())
                    .description(saved.getDescription())
                    .departmentId(saved.getDepartment() != null ? saved.getDepartment().getId() : null)
                    .address(saved.getAddress())
                    .latitude(saved.getLatitude())
                    .longitude(saved.getLongitude())
                    .photoUrl(saved.getPhotoUrl())
                    .voiceUrl(saved.getVoiceUrl())
                    .createdAt(saved.getCreatedAt().toString())
                    .priority(saved.getPriority().name())
                    .status(saved.getStatus().name())
                    .dueDate(saved.getDueDate().toString())
                    .assignedTo(saved.getAssignedTo() != null ? saved.getAssignedTo().getFullName() : null)
                    .assignedToDepartment(saved.getAssignedTo() != null && saved.getAssignedTo().getDepartment() != null ? saved.getAssignedTo().getDepartment().getName() : null)
                    .timeline(timeline)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new SimpleResponse(false, "Failed to create complaint: " + e.getMessage()));
        }
    }


    @GetMapping("/assigned-reports")
    public ResponseEntity<?> getAssignedReports(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User currentUser = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Only operators or department heads can have assigned reports
            if (currentUser.getRole() != Role.OPERATOR && currentUser.getRole() != Role.DEPARTMENT_HEAD) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            List<Complaint> complaints = complaintRepository.findByAssignedTo(currentUser);

            List<Map<String, Object>> response = complaints.stream().map(c -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", "R" + c.getId());
                dto.put("title", c.getTitle());
                dto.put("description", c.getDescription());
                dto.put("priority", c.getPriority() != null ? c.getPriority().name().toLowerCase() : "normal");
                dto.put("status", c.getStatus() != null ? c.getStatus().name().toLowerCase() : "assigned");
                dto.put("assignedDate", c.getAssignedAt() != null ? c.getAssignedAt().toLocalDate().toString() : null);
                dto.put("dueDate", c.getDueDate() != null ? c.getDueDate().toLocalDate().toString() : null);
                dto.put("submittedDate", c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : null);
                dto.put("department", currentUser.getDepartment() != null ? currentUser.getDepartment().getName() : null);
                dto.put("assignedTo", currentUser.getFullName() != null ? currentUser.getFullName() : "Current User");
                return dto;
            }).toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token or user not found"));
        }
    }


    @PostMapping("/assign")
    public ResponseEntity<?> assignReport(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> body) {

        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User currentUser = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Only SUPER_ADMIN or DEPARTMENT_HEAD can assign
            if (currentUser.getRole() != Role.SUPER_ADMIN && currentUser.getRole() != Role.DEPARTMENT_HEAD) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            String reportIdStr = body.get("reportId");
            String departmentIdStr = body.get("departmentId");
            String assignedToStr = body.get("assignedTo");

            if (reportIdStr == null || departmentIdStr == null || assignedToStr == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            Long reportId = Long.parseLong(reportIdStr);
            Long departmentId = Long.parseLong(departmentIdStr);
            Long assignedToId = Long.parseLong(assignedToStr);

            Complaint complaint = complaintRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

            User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("User to assign not found"));

            Department department = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            // Set assignment
            complaint.setAssignedTo(assignee);
            complaint.setAssignedAt(LocalDateTime.now());
            complaint.setStatus(ComplaintStatus.PENDING);
            complaintRepository.save(complaint);

            Map<String, Object> response = new HashMap<>();
            response.put("id",  complaint.getId());
            response.put("assignedTo", assignee.getFullName());
            response.put("department", department.getName());
            response.put("assignedAt", complaint.getAssignedAt());
            response.put("status", complaint.getStatus().name().toLowerCase());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getReportDetails(@PathVariable Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        // Prepare attachments list (example: photo + voice)
        List<Map<String, Object>> attachments = new ArrayList<>();
        if (complaint.getPhotoUrl() != null) {
            attachments.add(Map.of(
                    "id", 1,
                    "name", "photo",
                    "url", complaint.getPhotoUrl(),
                    "type", "image"
            ));
        }
        if (complaint.getVoiceUrl() != null) {
            attachments.add(Map.of(
                    "id", 2,
                    "name", "voice",
                    "url", complaint.getVoiceUrl(),
                    "type", "audio"
            ));
        }

        // Prepare timeline
        List<Map<String, Object>> timeline = new ArrayList<>();
        timeline.add(Map.of(
                "date", complaint.getCreatedAt() != null ? complaint.getCreatedAt().toLocalDate().toString() : null,
                "action", "Report submitted",
                "by", complaint.getUser() != null ? complaint.getUser().getFullName() : "Unknown"
        ));
        if (complaint.getAssignedTo() != null) {
            timeline.add(Map.of(
                    "date", complaint.getAssignedAt() != null ? complaint.getAssignedAt().toLocalDate().toString() : null,
                    "action", "Report assigned",
                    "by", complaint.getAssignedTo().getFullName()
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", "R" + complaint.getId());
        response.put("title", complaint.getTitle());
        response.put("description", complaint.getDescription());
        response.put("location", complaint.getAddress());
        response.put("coordinates", Map.of("lat", complaint.getLatitude(), "lng", complaint.getLongitude()));
        response.put("attachments", attachments);
        response.put("timeline", timeline);
        response.put("priority", complaint.getPriority() != null ? complaint.getPriority().name().toLowerCase() : "normal");
        response.put("department", complaint.getDepartment() != null ? complaint.getDepartment().getName() : null);
        response.put("estimatedResolution", complaint.getDueDate() != null ? complaint.getDueDate().toLocalDate().toString() : "N/A");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Complaint> updateStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status) {

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        complaint.setStatus(status);

        if (status == ComplaintStatus.RESOLVED) {
            complaint.setResolvedAt(LocalDateTime.now());
        } else {
            complaint.setResolvedAt(null); // optional: clear if status changes back
        }

        complaintRepository.save(complaint);

        return ResponseEntity.ok(complaint);
    }
    @GetMapping("/my-complaints")
    public ResponseEntity<?> getCitizenComplaints(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "status", required = false) ComplaintStatus status
    ) {
        try {
            String token = authHeader.substring(7);
            String phoneNumber = jwtUtil.extractPhoneNumber(token);

            User citizen = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new RuntimeException("Citizen not found"));

            if (citizen.getRole() != Role.CITIZEN) {
                return ResponseEntity.status(403).body(new SimpleResponse(false, "Only citizens can view their complaints"));
            }

            List<Complaint> complaints;
            if (status != null) {
                complaints = complaintRepository.findByUserAndStatus(citizen, status);
            } else {
                complaints = complaintRepository.findByUser(citizen);
            }

            // Convert to response DTOs
            List<ComplaintResponse> responses = complaints.stream().map(c -> ComplaintResponse.builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .description(c.getDescription())
                    .status(c.getStatus().name())
                    .priority(c.getPriority().name())
                    .createdAt(c.getCreatedAt().toString())
                    .dueDate(c.getDueDate() != null ? c.getDueDate().toString() : null)
                    .departmentId(c.getDepartment() != null ? c.getDepartment().getId() : null)
                    .assignedTo(c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : null)
                    .build()
            ).toList();

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new SimpleResponse(false, "Failed to fetch complaints: " + e.getMessage()));
        }
    }

    // ================== RESPONSE RECORD ==================
    record SimpleResponse(boolean success, String message) {}
}
