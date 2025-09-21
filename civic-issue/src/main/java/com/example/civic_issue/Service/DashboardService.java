package com.example.civic_issue.Service;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.dto.*;
import com.example.civic_issue.enums.ComplaintStatus;
import com.example.civic_issue.repo.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.List;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ComplaintRepository complaintRepository;

    public DashboardData getDashboardData() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        long totalReports = allComplaints.size();
        long pendingReports = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.PENDING)
                .count();
        long inProgressReports = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
        long resolvedReports = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .count();

        // Example change values, you can calculate based on last week/month
        String totalReportsChange = "+18.2%";
        String pendingReportsChange = "-5.5%";
        String inProgressReportsChange = "+10.0%";
        String resolvedReportsChange = "+25.0%";

        return new DashboardData(
                totalReports, totalReportsChange,
                pendingReports, pendingReportsChange,
                inProgressReports, inProgressReportsChange,
                resolvedReports, resolvedReportsChange
        );
    }

    public DashboardStatsResponse getDashboardStats() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        long total = allComplaints.size();
        long pending = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.PENDING)
                .count();
        long inProgress = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS)
                .count();
        long resolved = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED)
                .count();

        // Hardcoded example changes – later replace with real calculations
        Stat totalStat = new Stat(total, "+18.2% from last month", "up");
        Stat pendingStat = new Stat(pending, "-5.5% from last month", "down");
        Stat inProgressStat = new Stat(inProgress, "+10.0% from last month", "up");
        Stat resolvedStat = new Stat(resolved, "+25.0% from last month", "up");

        return new DashboardStatsResponse(totalStat, pendingStat, inProgressStat, resolvedStat);
    }


    public DashboardChartResponse getDashboardChartData() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        // ----- PIE DATA -----
        long pending = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.PENDING).count();
        long inProgress = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.IN_PROGRESS).count();
        long resolved = allComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();

        List<PieChartItem> pieData = List.of(
                new PieChartItem("Pending", pending, "#EF4444"),
                new PieChartItem("in-progress", inProgress, "#10B981"),
                new PieChartItem("Resolved", resolved, "#1F2937")
        );

        // ----- LINE DATA -----
        Map<String, Map<String, Long>> monthlyMap = new LinkedHashMap<>();
        // initialize months Jan-Jun
        for (int i = 1; i <= 6; i++) {
            String monthName = LocalDateTime.now().withMonth(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            monthlyMap.put(monthName, Map.of("Pending", 0L, "Resolved", 0L));
        }

        // Count complaints per month
        for (Complaint c : allComplaints) {
            LocalDateTime created = c.getCreatedAt();
            if (created != null && created.getMonthValue() <= 6) { // Jan-Jun
                String monthName = created.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                Map<String, Long> counts = new HashMap<>(monthlyMap.get(monthName));
                if (c.getStatus() == ComplaintStatus.PENDING) counts.put("Pending", counts.get("Pending") + 1);
                else if (c.getStatus() == ComplaintStatus.RESOLVED) counts.put("Resolved", counts.get("Resolved") + 1);
                monthlyMap.put(monthName, counts);
            }
        }

        List<LineChartItem> lineData = monthlyMap.entrySet().stream()
                .map(e -> new LineChartItem(e.getKey(), e.getValue().get("Pending"), e.getValue().get("Resolved")))
                .toList();

        return new DashboardChartResponse(pieData, lineData);
    }
    public List<RecentActivityItem> getRecentActivity() {
        List<Complaint> allComplaints = complaintRepository.findAll();

        // Sort complaints by createdAt descending
        allComplaints.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        List<RecentActivityItem> result = new ArrayList<>();
        int maxItems = 10; // limit to 10 recent activities

        for (Complaint c : allComplaints) {
            if (result.size() >= maxItems) break;

            String type = "report";
            String text = "New report submitted: " + c.getTitle();
            String icon = "📄";
            String color = "bg-blue-100 text-blue-600";

            if (c.getStatus() == ComplaintStatus.RESOLVED) {
                type = "resolved";
                text = "Report #" + c.getId() + " " + c.getTitle() + " resolved by " +
                        (c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : "Department");
                icon = "✅";
                color = "bg-green-100 text-green-600";
            }

            String timeAgo = formatTimeAgo(c.getCreatedAt());

            result.add(new RecentActivityItem(c.getId(), type, text, timeAgo, icon, color));
        }

        return result;
    }

    // Helper method to display "x minutes/hours ago"
    private String formatTimeAgo(LocalDateTime time) {
        if (time == null) return "Unknown";

        Duration duration = Duration.between(time, LocalDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes < 60) return minutes + " minutes ago";
        long hours = duration.toHours();
        if (hours < 24) return hours + " hours ago";
        long days = duration.toDays();
        return days + " days ago";
    }
}
