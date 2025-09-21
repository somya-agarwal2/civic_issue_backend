package com.example.civic_issue.repo;



import com.example.civic_issue.Model.WhatsAppSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppSessionRepository extends JpaRepository<WhatsAppSession, String> {
    // Optional: findByPhoneNumber is already handled by JpaRepository
}
