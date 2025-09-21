package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Stat {
    private long value;
    private String change; // e.g., "+18.2% from last month"
    private String trend;  // "up" or "down"
}

