package com.example.civic_issue.dto;

import lombok.Data;

@Data
public class DepartmentDetailsResponse {
    private Long id;
    private String name;
    private int openReports;
    private String avgResolutionTime;
    private int activeReports;
    private int resolvedLast30Days;
    private String manager;
    private String email;
    private String phone;
    private String address;
    private String description;
}
