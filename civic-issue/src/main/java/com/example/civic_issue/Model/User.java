package com.example.civic_issue.Model;

import com.example.civic_issue.enums.Role;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "users")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Location info for citizens
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastLocationUpdate;
    private String address;

    // Role: SUPER_ADMIN, DEPARTMENT_HEAD, OPERATOR, CITIZEN
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Password only needed for admin, heads, operators
    private String password;

    // Who created this user (Super Admin creates heads, heads create operators)
    @ManyToOne
    private User createdBy;

    // Department association: null for Super Admin and Citizens
    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonBackReference
    private Department department;

    // Complaints assigned to this user (operators or department head)
    @OneToMany(mappedBy = "assignedTo")
    private List<Complaint> assignedComplaints;

    // ✅ New fields
    private String email;           // Operator/Head email
    private String specialization;  // Operator specialization
    private String fullName;        // Optional: store name of the user (operator/head)
    private String addressDetail;   // Optional: alternate address field if needed

    // Add more fields here as required by frontend
}
