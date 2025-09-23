package com.example.civic_issue.Service;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.Department;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.repo.ComplaintRepository;
import com.example.civic_issue.repo.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final GeminiService geminiService;
    private final ComplaintRepository complaintRepository;
    private final DepartmentRepository departmentRepository;

    public Complaint createComplaint(User user, String title, String description, String category, String photoUrl, String voiceUrl) {
        Priority priority = geminiService.determinePriority(title, description, photoUrl);

        LocalDateTime dueDate = switch (priority) {
            case HIGH -> LocalDateTime.now().plusWeeks(1);
            case MEDIUM -> LocalDateTime.now().plusMonths(1);
            default -> LocalDateTime.now().plusMonths(3);
        };

        Complaint complaint = Complaint.builder()
                .title(title)
                .description(description)
                .category(category)
                .address(user.getAddress())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .photoUrl(photoUrl)
                .voiceUrl(voiceUrl)
                .priority(priority)
                .status(ComplaintStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .dueDate(dueDate)
                .user(user)
                .build();

        return complaintRepository.save(complaint);
    }

    public Complaint saveComplaint(Complaint complaint) {
        return complaintRepository.save(complaint);
    }

    public Priority getPriority(String title, String description, String photoUrl) {
        return geminiService.determinePriority(title, description, photoUrl);
    }
    public Department getDepartmentByName(String departmentName) {
        return departmentRepository.findByName(departmentName)
                .orElseThrow(() -> new RuntimeException(
                        "Department not found: " + departmentName
                ));
    }


    public Optional<Complaint> getComplaintById(Long id) {
        return complaintRepository.findById(id);
    }
}
