package com.example.civic_issue.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {
    private String departmentName;

    // Optional department head
    private String phoneNumber;
    private String password;
}
