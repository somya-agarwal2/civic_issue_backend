package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Model.WhatsAppSession;
import com.example.civic_issue.Service.ComplaintService;
import com.example.civic_issue.Service.DepartmentAssignmentService;
import com.example.civic_issue.Service.LocationService;
import com.example.civic_issue.Service.OtpService;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.repo.WhatsAppSessionRepository;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final UserRepository userRepository;
    private final WhatsAppSessionRepository sessionRepository;
    private final OtpService otpService;
    private final ComplaintService complaintService;
    private final DepartmentAssignmentService departmentAssignmentService;
    private final LocationService locationService;

    @PostMapping
    public String receiveMessage(HttpServletRequest request) {
        String msg = request.getParameter("Body");
        String from = request.getParameter("From"); // whatsapp:+91XXXXXX
        String latitude = request.getParameter("Latitude");
        String longitude = request.getParameter("Longitude");
        String mediaUrl = request.getParameter("MediaUrl0");

        Optional<User> optionalUser = userRepository.findByPhoneNumber(from);
        User user = optionalUser.orElseGet(() -> User.builder()
                .phoneNumber(from)
                .role(Role.CITIZEN)
                .build());
        userRepository.save(user);

        // Fetch or create WhatsApp session from DB
        WhatsAppSession session = sessionRepository.findById(from)
                .orElseGet(() -> WhatsAppSession.builder()
                        .phoneNumber(from)
                        .step("NEW")
                        .build());

        MessagingResponse twiml = null;

        try {
            switch (session.getStep()) {

                case "NEW" -> {
                    otpService.generateAndSendOtp(user.getPhoneNumber());
                    session.setStep("WAIT_OTP");
                    sessionRepository.save(session);
                    twiml = buildMessage(
                            "👋 Welcome to CivicSense!\n" +
                                    "We've sent an OTP via SMS. Please reply here with the OTP."
                    );
                }

                case "WAIT_OTP" -> {
                    String token = otpService.verifyOtpAndGetToken(user.getPhoneNumber(), msg.trim());
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
                    if (!msg.equalsIgnoreCase("Skip")) session.setTempPhotoUrl(mediaUrl);
                    session.setStep("ASK_VOICE");
                    sessionRepository.save(session);
                    twiml = buildMessage("🎤 You can upload a voice note (optional). Send 'Skip' to continue.");
                }

                case "ASK_VOICE" -> {
                    if (!msg.equalsIgnoreCase("Skip")) session.setTempVoiceUrl(mediaUrl);

                    // Build complaint object
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

                default -> twiml = buildMessage("👋 Welcome to CivicSense! Please send any message to start.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            twiml = buildMessage("⚠️ Something went wrong. Please try again.");
        }

        // Handle checked exception here
        try {
            return twiml.toXml();
        } catch (Exception e) {
            e.printStackTrace();
            return "<Response><Message>⚠️ Something went wrong. Please try again.</Message></Response>";
        }
    }

    private MessagingResponse buildMessage(String text) {
        Body body = new Body.Builder(text).build();
        Message message = new Message.Builder().body(body).build();
        return new MessagingResponse.Builder().message(message).build();
    }
}

