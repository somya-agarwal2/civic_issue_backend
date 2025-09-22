package com.example.civic_issue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UserInfo user;  // put first for frontend

    private String token;   // token comes second

    @Data
    @NoArgsConstructor

    public static class UserInfo {
        private Long id;
        private String fullName;
        private String email;
        private String role;
        private String department;
        private Long departmentId;
        public UserInfo(Long id, String fullName, String email, String role, String department, Long departmentId) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.department = department;
            this.departmentId = departmentId;
        }
    }
}
