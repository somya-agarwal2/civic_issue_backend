package com.example.civic_issue.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AdminLoginRequest {
    private String phoneNumber;
    private String password;
}
