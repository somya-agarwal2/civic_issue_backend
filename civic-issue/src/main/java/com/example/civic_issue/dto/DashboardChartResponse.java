package com.example.civic_issue.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DashboardChartResponse {
    private List<PieChartItem> pieData;
    private List<LineChartItem> lineData;
}
