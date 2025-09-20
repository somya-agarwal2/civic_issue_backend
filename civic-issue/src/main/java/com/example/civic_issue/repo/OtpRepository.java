package com.example.civic_issue.repo;




import com.example.civic_issue.Model.OtpStore;
import com.example.civic_issue.Model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpStore, Long> {
    Optional<OtpStore> findByPhoneNumberAndOtpCode(String phoneNumber, String otpCode);
    @Transactional
    void deleteByPhoneNumber(String phoneNumber);

    @Transactional
    void deleteByExpiryTimeBefore(LocalDateTime now);

}