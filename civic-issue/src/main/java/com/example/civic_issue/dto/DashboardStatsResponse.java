package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {
    private Stat totalReports;
    private Stat pendingReports;
    private Stat inProgress;
    private Stat resolved;
}
