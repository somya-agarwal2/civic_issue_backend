package com.example.civic_issue.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store")
@Data

@Getter
@Setter // generates getters, setters, equals, hashCode, toString
@NoArgsConstructor  // generates a no-args constructor (needed by JPA)
@AllArgsConstructor // generates an all-args constructor
@Builder            // enables OtpStore.builder() style creation
public class OtpStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;

    private String otpCode;

    private LocalDateTime expiryTime;
}
