package com.example.civic_issue.dto;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String phoneNumber;
    private String password;
}
