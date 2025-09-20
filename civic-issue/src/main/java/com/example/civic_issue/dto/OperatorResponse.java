package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OperatorResponse {
    private Long id;
    private String phoneNumber;
    private String departmentName;
}
