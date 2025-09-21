package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardData {
    private long totalReports;
    private String totalReportsChange;

    private long pendingReports;
    private String pendingReportsChange;

    private long inProgressReports;
    private String inProgressReportsChange;

    private long resolvedReports;
    private String resolvedReportsChange;
}
