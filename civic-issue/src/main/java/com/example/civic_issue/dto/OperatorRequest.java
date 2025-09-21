package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorRequest {
    private String operatorName;
    private String phoneNumber;
    private String password;

    // New fields requested by frontend
    private String email;
    private String specialization;

    // You can add more optional fields as needed
}
