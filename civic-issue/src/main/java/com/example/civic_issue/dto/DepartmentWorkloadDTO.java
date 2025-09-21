package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentWorkloadDTO {
    private String department;
    private long active;
    private long pending;
    private long resolved;
}
