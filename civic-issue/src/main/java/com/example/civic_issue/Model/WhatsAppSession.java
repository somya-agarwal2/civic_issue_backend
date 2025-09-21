package com.example.civic_issue.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppSession {

    @Id
    private String phoneNumber; // WhatsApp number as primary key

    private String step; // e.g., NEW, WAIT_OTP, ASK_LOCATION, etc.

    private String tempTitle;
    private String tempDescription;
    private String tempCategory;
    private String tempPhotoUrl;
    private String tempVoiceUrl;
}
