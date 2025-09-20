package com.example.civic_issue.dto;

import com.example.civic_issue.enums.ComplaintStatus;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private ComplaintStatus status;
}
