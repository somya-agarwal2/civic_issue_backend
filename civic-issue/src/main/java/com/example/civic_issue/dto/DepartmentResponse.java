package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private String name;

    // Department statistics
    private int openReports;           // Currently active / pending reports
    private String avgResolutionTime;  // e.g., "2.5 days"
    private int activeReports;         // Number of ongoing reports
    private int resolvedLast30Days;    // Number of reports resolved in last 30 days

    // Head / manager info
    private String manager;   // Full name of department head
    private String email;     // Head email
    private String phone;     // Head phone number
    private String address;   // Department physical address

    private String description; // Department description
}
