package com.example.civic_issue.Service;

import com.example.civic_issue.Model.OtpStore;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.OtpRepository;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class WhatsAppOtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${twilio.account.sid}")
    private String ACCOUNT_SID;

    @Value("${twilio.auth.token}")
    private String AUTH_TOKEN;

    @Value("${twilio.phone.number}")  // SMS-capable Twilio number
    private String TWILIO_PHONE;

    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Generate OTP, save it in DB, and send **via SMS**.
     */
    public void generateAndSendOtp(String phoneNumber) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // Delete previous OTP for this phone
        otpRepository.deleteByPhoneNumber(phoneNumber);

        // Save new OTP
        OtpStore otpEntry = OtpStore.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();
        otpRepository.save(otpEntry);

        // Send OTP via Twilio SMS
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message.creator(
                new PhoneNumber(phoneNumber),   // To recipient (SMS)
                new PhoneNumber(TWILIO_PHONE), // From SMS-capable Twilio number
                "Your OTP for Civic App is: " + otp
        ).create();

        System.out.println("OTP sent via SMS to " + phoneNumber);
    }

    /**
     * Verify OTP and return JWT token if valid.
     */
    @Transactional
    public String verifyOtpAndGetToken(String phoneNumber, String otpCode) {
        Optional<OtpStore> otpRecord = otpRepository.findByPhoneNumberAndOtpCode(phoneNumber, otpCode);

        if (otpRecord.isPresent() && otpRecord.get().getExpiryTime().isAfter(LocalDateTime.now())) {

            // Ensure user exists
            userRepository.findByPhoneNumber(phoneNumber)
                    .orElseGet(() -> userRepository.save(
                            User.builder()
                                    .phoneNumber(phoneNumber)
                                    .role(Role.CITIZEN)
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    ));

            // Delete used OTP
            otpRepository.deleteByPhoneNumber(phoneNumber);

            // Generate JWT token
            return jwtUtil.generateTokenWithRole(phoneNumber, Role.CITIZEN);
        }

        return null; // Invalid or expired OTP
    }
}
