package com.example.civic_issue.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
@Data
@Getter
@Setter
public class ComplaintRequest {
    private String title;
    private String description;
    private Long departmentId;  // String from frontend
    private MultipartFile photo; // optional
    private MultipartFile voice; // optional
}

