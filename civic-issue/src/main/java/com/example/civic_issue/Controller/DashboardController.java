package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.Service.DashboardService;
import com.example.civic_issue.dto.DashboardChartResponse;
import com.example.civic_issue.dto.DashboardData;
import com.example.civic_issue.dto.DashboardStatsResponse;
import com.example.civic_issue.dto.RecentActivityItem;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardData(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String phone = jwtUtil.extractPhoneNumber(token);

        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only super admins can access full dashboard
        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        DashboardData data = dashboardService.getDashboardData();
        return ResponseEntity.ok(data);
    }
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String phone = jwtUtil.extractPhoneNumber(token);

        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/chart-data")
    public ResponseEntity<?> getDashboardChartData(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String phone = jwtUtil.extractPhoneNumber(token);

        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getRole() != Role.SUPER_ADMIN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        DashboardChartResponse response = dashboardService.getDashboardChartData();
        return ResponseEntity.ok(response);
    }
    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<?> getRecentActivity(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String phone = jwtUtil.extractPhoneNumber(token);

        User admin = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only allow admins / department heads / operators
        if (admin.getRole() == Role.CITIZEN) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<RecentActivityItem> activity = dashboardService.getRecentActivity();
        return ResponseEntity.ok(activity);
    }


}
