package com.example.civic_issue.dto;

import com.example.civic_issue.enums.Category;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ComplaintRequest {
    private String title;
    private String description;
    private String category; // String from frontend
    private String photoBase64; // optional
    private String voiceBase64; // optional
}

