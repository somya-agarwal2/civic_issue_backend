package com.example.civic_issue.Service;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.repo.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final GeminiService geminiService;
    private final ComplaintRepository complaintRepository;

    /**
     * Handles creating a complaint with:
     * - AI-based priority
     * - Due date calculation
     * - Status set to PENDING
     *
     * @param user        The citizen filing the complaint
     * @param title       Complaint title
     * @param description Complaint description
     * @param category    Complaint category
     * @param photoUrl    Optional photo URL
     * @param voiceUrl    Optional voice URL
     * @return Saved Complaint
     */
    public Complaint createComplaint(User user, String title, String description, String category, String photoUrl, String voiceUrl) {

        // Determine priority using AI service
        Priority priority = geminiService.determinePriority(title, description, photoUrl);

        // Calculate due date based on priority
        LocalDateTime dueDate;
        switch (priority) {
            case HIGH -> dueDate = LocalDateTime.now().plusWeeks(1);
            case MEDIUM -> dueDate = LocalDateTime.now().plusMonths(1);
            default -> dueDate = LocalDateTime.now().plusMonths(3);
        }

        // Build complaint object
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
                .status(ComplaintStatus.PENDING) // always pending initially
                .createdAt(LocalDateTime.now())
                .dueDate(dueDate)
                .user(user)
                .build();

        // Save and return
        return complaintRepository.save(complaint);
    }
}
