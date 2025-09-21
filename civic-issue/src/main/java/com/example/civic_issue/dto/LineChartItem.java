package com.example.civic_issue.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data

@AllArgsConstructor
public class LineChartItem {
    private String name; // e.g., "Jan"
    private long Pending;
    private long Resolved;
}