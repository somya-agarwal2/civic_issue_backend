package com.example.civic_issue.dto;



import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecentActivityItem {
    private Long id;
    private String type;  // "report", "resolved", etc.
    private String text;
    private String time;  // human-readable, e.g., "2 minutes ago"
    private String icon;
    private String color;
}
