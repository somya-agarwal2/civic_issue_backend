package com.example.civic_issue.Model;

import com.example.civic_issue.enums.Priority;
import com.example.civic_issue.enums.ComplaintStatus;
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
    private User user; // Citizen who created

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo; // Department head/operator

}
