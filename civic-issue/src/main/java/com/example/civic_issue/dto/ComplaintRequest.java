package com.example.civic_issue.dto;

import com.example.civic_issue.enums.Category;
import lombok.Data;

@Data
public class ComplaintRequest {
    private String title;
    private String description;
    private String category;// keep as String from frontend
}
