package com.example.civic_issue.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Transient
    private String confirmpassword;

    private String newPassword;
   private String confirmPassword;
    private String role = "USER";

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private boolean profileCompleted = false;

    private Double latitude;
    private Double longitude;

    private String photoUrl;
}
