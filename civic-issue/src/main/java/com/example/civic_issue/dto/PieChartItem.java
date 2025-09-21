package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PieChartItem {
    private String name;
    private long value;
    private String fill;
}

