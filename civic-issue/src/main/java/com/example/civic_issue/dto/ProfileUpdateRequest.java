package com.example.civic_issue.dto;

public record ProfileUpdateRequest (
     String firstName,
    String lastName,
     String phoneNumber,
  String address
    // Getters & Setters
)
{}