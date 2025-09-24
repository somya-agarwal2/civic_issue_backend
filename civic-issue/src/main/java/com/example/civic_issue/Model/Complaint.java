package com.example.civic_issue.Model;

import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.enums.ComplaintStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    // Example:
    private String category;  // ✅ Should be String

    @ManyToOne
    @JsonIgnoreProperties({"head"}) // prevent looping into head -> assignedComplaints
    private Department department;

    private String address;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String voiceUrl;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private LocalDateTime assignedAt;
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"assignedComplaints", "password"}) // prevent loops & hide sensitive data
    private User user;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_id")
    @JsonIgnoreProperties({"assignedComplaints", "password"})
    private User assignedTo;
}
