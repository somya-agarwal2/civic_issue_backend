package com.example.civic_issue.enums;


public enum ComplaintStatus {
    PENDING,      // Newly filed
    IN_PROGRESS,  // Assigned to operator and work started
    RESOLVED,     // Complaint resolved
    REJECTED      // If not valid or cannot be resolved
}
