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
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // Twilio credentials loaded from application.properties
    @Value("${twilio.account.sid}")
    private String ACCOUNT_SID;

    @Value("${twilio.auth.token}")
    private String AUTH_TOKEN;

    @Value("${twilio.phone.number}")
    private String TWILIO_PHONE;

    public void generateAndSendOtp(String phoneNumber) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        // clear old otp for this phone
        otpRepository.deleteByPhoneNumber(phoneNumber);

        // save new otp
        OtpStore otpEntry = OtpStore.builder()
                .phoneNumber(phoneNumber)
                .otpCode(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))

                .build();
        otpRepository.save(otpEntry);

        // Initialize Twilio and send OTP
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        Message.creator(
                new PhoneNumber(phoneNumber),   // to
                new PhoneNumber(TWILIO_PHONE), // from (your Twilio number)
                "Your OTP for Civic App is: " + otp
        ).create();

        System.out.println("OTP sent to " + phoneNumber); // optional log
    }

    @Transactional
    public String verifyOtpAndGetToken(String phoneNumber, String otpCode) {
        Optional<OtpStore> otpRecord =
                otpRepository.findByPhoneNumberAndOtpCode(phoneNumber, otpCode);

        if (otpRecord.isPresent() && otpRecord.get().getExpiryTime().isAfter(LocalDateTime.now())) {
            // ensure user exists
            userRepository.findByPhoneNumber(phoneNumber)
                    .orElseGet(() -> userRepository.save(
                            User.builder().phoneNumber(phoneNumber).
                            role(Role.CITIZEN).build()
                    ));

            otpRepository.deleteByPhoneNumber(phoneNumber); // clear used otp

            // generate JWT token
            return jwtUtil.generateTokenWithRole(phoneNumber, Role.CITIZEN);
        }

        return null; // invalid or expired OTP
    }
}
