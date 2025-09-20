package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime lastUpdated;
}
