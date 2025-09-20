package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private String name;
    private String headPhoneNumber;
    private int operatorCount;
}
