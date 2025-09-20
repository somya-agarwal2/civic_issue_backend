package com.example.civic_issue.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String address;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String voiceUrl;
    private String createdAt;
    private String priority; // enum type
    private String status;   // ComplaintStatus
    private String dueDate;

    // New fields
      // Name of department head/operator
    private String assignedToDepartment; // Department name
}
