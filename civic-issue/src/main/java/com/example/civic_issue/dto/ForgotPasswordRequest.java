package com.example.civic_issue.dto;

public record ForgotPasswordRequest(String email, String newPassword, String confirmPassword) {}
