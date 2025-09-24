package com.example.civic_issue.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ComplaintResponse {

    private Long id;
    private String title;
    private String description;
    private Long departmentId;       // Use the actual department ID
    private String departmentName;   // Include department name dynamically
    private String address;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String voiceUrl;
    // Add this field
    private String resolvedAt;

    private String createdAt;
    private String priority; // enum type
    private String status;   // ComplaintStatus
    private String dueDate;

    private String assignedTo;            // Name of assigned operator/department head
    private String assignedToDepartment;  // Department name of assignee

    private List<TimelineEvent> timeline; // Timeline events for frontend

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimelineEvent {
        private String date;
        private String action;
        private String by; // Who performed the action
    }
}
