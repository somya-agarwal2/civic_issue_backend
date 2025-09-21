package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Service.ComplaintService;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;

    @PostMapping
    public String receiveMessage(HttpServletRequest request) {
        String msg = request.getParameter("Body"); // incoming text
        String from = request.getParameter("From"); // WhatsApp sender number

        // Check if user exists, else create new citizen
        Optional<User> optionalUser = userRepository.findByPhoneNumber(from);
        User user = optionalUser.orElseGet(() -> {
            User newUser = new User();
            newUser.setPhoneNumber(from);
            newUser.setRole(Role.CITIZEN); // default role for WhatsApp users
            return userRepository.save(newUser);
        });

        MessagingResponse twiml;

        try {
            if (msg.toLowerCase().startsWith("report:")) {
                // Example message:
                // Report: Pothole; Description: Big pothole on 5th street; Category: Road
                String[] parts = msg.substring(7).split(";");
                String title = parts[0].trim();
                String description = parts.length > 1 ? parts[1].replaceFirst("Description:", "").trim() : "";
                String category = parts.length > 2 ? parts[2].replaceFirst("Category:", "").trim() : "General";

                Complaint complaint = complaintService.createComplaint(
                        user, title, description, category, null, null
                );

                Body responseBody = new Body.Builder(
                        "✅ Complaint created!\nID: " + complaint.getId() +
                                "\nStatus: " + complaint.getStatus() +
                                "\nPriority: " + complaint.getPriority()
                ).build();
                Message message = new Message.Builder().body(responseBody).build();
                twiml = new MessagingResponse.Builder().message(message).build();

            } else if (msg.toLowerCase().startsWith("status:")) {
                Long complaintId = Long.parseLong(msg.substring(7).trim());
                Optional<Complaint> complaintOpt = complaintService.getComplaintById(complaintId);

                String statusMsg = complaintOpt
                        .map(c -> "📌 Complaint " + complaintId + " status: " + c.getStatus())
                        .orElse("Complaint ID not found.");

                Body responseBody = new Body.Builder(statusMsg).build();
                Message message = new Message.Builder().body(responseBody).build();
                twiml = new MessagingResponse.Builder().message(message).build();

            } else {
                Body responseBody = new Body.Builder(
                        "👋 Welcome to CivicBot!\n\n" +
                                "To report an issue:\nReport: <title>; Description: <desc>; Category: <cat>\n\n" +
                                "To check status:\nStatus: <complaintId>"
                ).build();
                Message message = new Message.Builder().body(responseBody).build();
                twiml = new MessagingResponse.Builder().message(message).build();
            }

        } catch (Exception e) {
            Body responseBody = new Body.Builder("⚠️ Error. Please check message format.").build();
            Message message = new Message.Builder().body(responseBody).build();
            twiml = new MessagingResponse.Builder().message(message).build();
        }

        return twiml.toXml();
    }
}
