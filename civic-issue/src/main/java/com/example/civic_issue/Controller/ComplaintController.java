package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Service.CloudinaryService;
import com.example.civic_issue.Service.DepartmentAssignmentService;
import com.example.civic_issue.Service.GeminiService;
import com.example.civic_issue.Service.LocationService;
import com.example.civic_issue.dto.ComplaintRequest;
import com.example.civic_issue.dto.ComplaintResponse;
import com.example.civic_issue.dto.UpdateStatusRequest;
import com.example.civic_issue.enums.Category;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

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

    @PostMapping("/create")
    public ResponseEntity<?> createComplaint(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ComplaintRequest complaintRequest, // <- use @RequestBody
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "voice", required = false) MultipartFile voice
    ) {
        String token = authHeader.substring(7);
        String phoneNumber = jwtUtil.extractPhoneNumber(token);

        User citizen = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        if (citizen.getRole() != Role.CITIZEN) {
            return ResponseEntity.status(403).body("Only citizens can file complaints");
        }

        // ✅ Convert String to Enum
        Category categoryEnum = Category.fromString(complaintRequest.getCategory());
        if (categoryEnum == null) {
            throw new RuntimeException("Invalid category: " + complaintRequest.getCategory());
        }

        // Upload files
        String photoUrl = (photo != null) ? cloudinaryService.uploadFile(photo, "complaints/photos") : null;
        String voiceUrl = (voice != null) ? cloudinaryService.uploadFile(voice, "complaints/voices") : null;

        // Determine priority
        Priority priority = geminiService.determinePriority(
                complaintRequest.getTitle(),
                complaintRequest.getDescription(),
                photoUrl
        );

        // Calculate due date
        LocalDateTime dueDate;
        if (priority == Priority.HIGH) dueDate = LocalDateTime.now().plusWeeks(1);
        else if (priority == Priority.MEDIUM) dueDate = LocalDateTime.now().plusMonths(1);
        else dueDate = LocalDateTime.now().plusMonths(3);

        String fetchedAddress = locationService.getAddressFromCoordinates(
                citizen.getLatitude(),
                citizen.getLongitude()
        );

        // Store category as String (enum name or displayName)
        Complaint complaint = Complaint.builder()
                .title(complaintRequest.getTitle())
                .description(complaintRequest.getDescription())
                .category(categoryEnum.getDisplayName()) // ✅ save displayName for frontend
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

        // Assign department head
        User departmentHead = departmentAssignmentService.getDepartmentHeadForCategory(categoryEnum.getDepartmentName());
        if (departmentHead != null) {
            complaint.setAssignedTo(departmentHead);
            complaint.setAssignedAt(LocalDateTime.now());
        }

        Complaint saved = complaintRepository.save(complaint);

        // Build response
        ComplaintResponse response = ComplaintResponse.builder()
                .id(saved.getId())
                .title(saved.getTitle())
                .description(saved.getDescription())
                .category(saved.getCategory()) // ✅ already String
                .address(saved.getAddress())
                .latitude(saved.getLatitude())
                .longitude(saved.getLongitude())
                .photoUrl(saved.getPhotoUrl())
                .voiceUrl(saved.getVoiceUrl())
                .createdAt(saved.getCreatedAt().toString())
                .priority(saved.getPriority() != null ? saved.getPriority().name() : null)
                .status(saved.getStatus().name())
                .dueDate(saved.getDueDate() != null ? saved.getDueDate().toString() : null)

                .assignedToDepartment(saved.getAssignedTo() != null && saved.getAssignedTo().getDepartment() != null
                        ? saved.getAssignedTo().getDepartment().getName()
                        : null)
                .build();

        return ResponseEntity.ok(response);
    }
    @PutMapping("/update-status/{complaintId}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long complaintId,
            @RequestBody UpdateStatusRequest request
    ) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        String phone = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User loggedUser = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = loggedUser.getRole();

        if (role == Role.OPERATOR || role == Role.DEPARTMENT_HEAD || role == Role.SUPER_ADMIN) {
            complaint.setStatus(request.getStatus());
            if (request.getStatus() == ComplaintStatus.RESOLVED) {
                complaint.setResolvedAt(LocalDateTime.now());
            }
            complaintRepository.save(complaint);

            ComplaintResponse response = ComplaintResponse.builder()
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

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).body("Access denied");
        }
    }

}
