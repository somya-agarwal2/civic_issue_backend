package com.example.civic_issue.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ComplaintRequest {
    private String title;
    private String description;
    private Long departmentId;  // String from frontend
    private String photoBase64; // optional
    private String voiceBase64; // optional
}

