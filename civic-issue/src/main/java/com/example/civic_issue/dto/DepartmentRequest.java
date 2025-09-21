package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    private String name;             // Department name
    private String description;      // Department description
    private String departmentHead;   // Full name of department head
    private String email;            // Head email
    private String phone;            // Head phone number
    private String address;          // Department address
    private String password;         // Password for department head

    // Optional: Add any additional fields required by frontend, e.g., role, timezone, etc.
}
