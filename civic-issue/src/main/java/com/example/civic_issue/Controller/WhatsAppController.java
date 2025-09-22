package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Model.WhatsAppSession;
import com.example.civic_issue.Service.*;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.repo.WhatsAppSessionRepository;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Add these imports at the top of your WhatsAppController
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;



@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {
    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;


    private final UserRepository userRepository;
    private final WhatsAppSessionRepository sessionRepository;
    private final WhatsAppOtpService whatsAppOtpService;
    private final ComplaintService complaintService;
    private final DepartmentAssignmentService departmentAssignmentService;
    private final LocationService locationService;
    private final WhatsAppCloudinaryService cloudinaryService;

    @PostMapping
    public ResponseEntity<String> receiveMessage(HttpServletRequest request) {
        String msg = request.getParameter("Body");
        String rawFrom = request.getParameter("From"); // e.g. whatsapp:+91XXXXXX
        String phone = rawFrom.replace("whatsapp:", "").trim(); // normalize
        String latitude = request.getParameter("Latitude");
        String longitude = request.getParameter("Longitude");
        String mediaUrl = request.getParameter("MediaUrl0");

        System.out.println("Incoming WhatsApp message:");
        System.out.println("From: " + rawFrom);
        System.out.println("Body: " + msg);
        System.out.println("Latitude: " + latitude + ", Longitude: " + longitude);
        System.out.println("MediaUrl0: " + mediaUrl);

        // Ensure user exists
        User user = userRepository.findByPhoneNumber(phone)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .phoneNumber(phone)
                                .role(Role.CITIZEN)
                                .createdAt(LocalDateTime.now())
                                .build()
                ));

        // Reset session if user wants to start fresh
        WhatsAppSession session;
        if (msg.equalsIgnoreCase("hi") || msg.equalsIgnoreCase("hello")) {
            sessionRepository.deleteById(phone);
            session = sessionRepository.save(
                    WhatsAppSession.builder()
                            .phoneNumber(phone)
                            .step("NEW")
                            .tempTitle(null)
                            .tempDescription(null)
                            .tempCategory(null)
                            .tempPhotoUrl(null)
                            .tempVoiceUrl(null)
                            .build()
            );
        } else {
            session = sessionRepository.findById(phone)
                    .orElseGet(() -> sessionRepository.save(
                            WhatsAppSession.builder()
                                    .phoneNumber(phone)
                                    .step("NEW")
                                    .build()
                    ));
        }

        MessagingResponse twiml;

        try {
            switch (session.getStep()) {
                case "NEW" -> {
                    whatsAppOtpService.generateAndSendOtp(phone);
                    session.setStep("WAIT_OTP");
                    sessionRepository.save(session);
                    twiml = buildMessage(
                            "👋 Welcome to CivicSense!\n" +
                                    "We've sent an OTP to your phone via SMS. Please reply here with the OTP."
                    );
                }

                case "WAIT_OTP" -> {
                    String token = whatsAppOtpService.verifyOtpAndGetToken(phone, msg.trim());
                    if (token != null) {
                        session.setStep("ASK_LOCATION");
                        sessionRepository.save(session);
                        twiml = buildMessage("✅ OTP verified!\n📍 Please share your live location.");
                    } else {
                        twiml = buildMessage("❌ Invalid or expired OTP. Please try again.");
                    }
                }

                case "ASK_LOCATION" -> {
                    if (latitude != null && longitude != null) {
                        user.setLatitude(Double.parseDouble(latitude));
                        user.setLongitude(Double.parseDouble(longitude));
                        userRepository.save(user);

                        session.setStep("ASK_TITLE");
                        sessionRepository.save(session);
                        twiml = buildMessage("📝 Please enter the title of your complaint.");
                    } else {
                        twiml = buildMessage("⚠️ Please share your live location to proceed.");
                    }
                }

                case "ASK_TITLE" -> {
                    session.setTempTitle(msg.trim());
                    session.setStep("ASK_DESCRIPTION");
                    sessionRepository.save(session);
                    twiml = buildMessage("✍️ Please enter the description of your complaint.");
                }

                case "ASK_DESCRIPTION" -> {
                    session.setTempDescription(msg.trim());
                    session.setStep("ASK_CATEGORY");
                    sessionRepository.save(session);
                    twiml = buildMessage(
                            "Please select category by number:\n" +
                                    "1. Water & Sewerage\n" +
                                    "2. Electricity & Power\n" +
                                    "3. Roads & Transport\n" +
                                    "4. Sanitation & Waste\n" +
                                    "5. Health & Public Safety"
                    );
                }

                case "ASK_CATEGORY" -> {
                    String category = switch (msg.trim()) {
                        case "1" -> "Water & Sewerage";
                        case "2" -> "Electricity & Power";
                        case "3" -> "Roads & Transport";
                        case "4" -> "Sanitation & Waste";
                        case "5" -> "Health & Public Safety";
                        default -> null;
                    };

                    if (category == null) {
                        twiml = buildMessage("❌ Invalid selection. Please choose 1-5.");
                    } else {
                        session.setTempCategory(category);
                        session.setStep("ASK_PHOTO");
                        sessionRepository.save(session);
                        twiml = buildMessage("📸 You can upload a photo (optional). Send 'Skip' to continue.");
                    }
                }

                case "ASK_PHOTO" -> {
                    if (!msg.equalsIgnoreCase("Skip") && mediaUrl != null) {
                        InputStream inputStream = getTwilioMedia(mediaUrl, twilioAccountSid, twilioAuthToken);
                        String uploadedUrl = cloudinaryService.uploadFile(inputStream, "complaints/photos");
                        session.setTempPhotoUrl(uploadedUrl);
                    }
                    session.setStep("ASK_VOICE");
                    sessionRepository.save(session);
                    twiml = buildMessage("🎤 You can upload a voice note (optional). Send 'Skip' to continue.");
                }

                case "ASK_VOICE" -> {
                    if (!msg.equalsIgnoreCase("Skip") && mediaUrl != null) {
                        InputStream inputStream = getTwilioMedia(mediaUrl, twilioAccountSid, twilioAuthToken);
                        String uploadedUrl = cloudinaryService.uploadFile(inputStream, "complaints/voice");
                        session.setTempVoiceUrl(uploadedUrl); // ✅ Correct property
                    }

                    // Save complaint
                    Complaint complaint = Complaint.builder()
                            .title(session.getTempTitle())
                            .description(session.getTempDescription())
                            .category(session.getTempCategory())
                            .latitude(user.getLatitude())
                            .longitude(user.getLongitude())
                            .address(locationService.getAddressFromCoordinates(user.getLatitude(), user.getLongitude()))
                            .photoUrl(session.getTempPhotoUrl())
                            .voiceUrl(session.getTempVoiceUrl())
                            .priority(complaintService.getPriority(
                                    session.getTempTitle(),
                                    session.getTempDescription(),
                                    session.getTempPhotoUrl()))
                            .status(com.example.civic_issue.enums.ComplaintStatus.PENDING)
                            .createdAt(LocalDateTime.now())
                            .user(user)
                            .build();

                    User head = departmentAssignmentService.getDepartmentHeadForCategory(session.getTempCategory());
                    if (head != null) {
                        complaint.setAssignedTo(head);
                        complaint.setAssignedAt(LocalDateTime.now());
                    }

                    complaintService.saveComplaint(complaint);

                    session.setStep("DONE");
                    sessionRepository.save(session);

                    twiml = buildMessage(
                            "✅ Complaint submitted!\nID: " + complaint.getId() +
                                    "\nStatus: " + complaint.getStatus() +
                                    "\nPriority: " + complaint.getPriority()
                    );
                }

                case "DONE" -> {
                    twiml = buildMessage("👋 Your previous complaint has been submitted. Type 'hi' to start a new complaint.");
                }

                default -> twiml = buildMessage("👋 Welcome to CivicSense! Please send any message to start.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            twiml = buildMessage("⚠️ Something went wrong. Please try again.");
        }

        String responseXml;
        try {
            responseXml = twiml.toXml();
        } catch (Exception e) {
            e.printStackTrace();
            responseXml = "<Response><Message>⚠️ Something went wrong. Please try again.</Message></Response>";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/xml");

        return new ResponseEntity<>(responseXml, headers, HttpStatus.OK);
    }
    private InputStream getTwilioMedia(String mediaUrl, String accountSid, String authToken) throws Exception {
        URL url = new URL(mediaUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String authStr = accountSid + ":" + authToken;
        String encodedAuth = Base64.getEncoder().encodeToString(authStr.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

        return conn.getInputStream();
    }

    private MessagingResponse buildMessage(String text) {
        Body body = new Body.Builder(text).build();
        Message message = new Message.Builder().body(body).build();
        return new MessagingResponse.Builder().message(message).build();
    }
}

