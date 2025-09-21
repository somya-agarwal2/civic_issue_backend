package com.example.civic_issue.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationState {
    private Long userId;
    private String step; // OTP, LOCATION, TITLE, DESCRIPTION, CATEGORY, PHOTO, VOICE, DONE
    private String tempTitle;
    private String tempDescription;
    private String tempCategory;
    private String tempPhotoUrl;
    private String tempVoiceUrl;
    private String otp;
}
