package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.Complaint;
import com.example.civic_issue.repo.ComplaintRepository;

import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ComplaintRepository complaintRepository;

    private final Path exportDir = Paths.get("exports"); // store generated files

    @PostMapping("/reports/{id}")
    public ResponseEntity<?> exportReportData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String format
    ) {
        try {
            Complaint complaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

            if (!Files.exists(exportDir)) {
                Files.createDirectories(exportDir);
            }

            String fileName = String.format("report-R%s-%s.%s",
                    complaint.getId(),
                    LocalDate.now(),
                    format.toLowerCase()
            );

            Path filePath = exportDir.resolve(fileName);

            if (format.equalsIgnoreCase("pdf")) {
                generatePdf(complaint, filePath.toFile());
            } else if (format.equalsIgnoreCase("csv")) {
                generateCsv(complaint, filePath.toFile());
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Unsupported format"));
            }

            String downloadUrl = "/api/exports/download/" + fileName;
            return ResponseEntity.ok(Map.of(
                    "fileName", fileName,
                    "success", true,
                    "downloadUrl", downloadUrl
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ---------------- PDF generation ----------------
    private void generatePdf(Complaint complaint, File file) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDPageContentStream content = new PDPageContentStream(doc, page);
            content.beginText();
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.setLeading(18f);
            content.newLineAtOffset(50, 700);
            content.showText("Report ID: R" + complaint.getId());
            content.newLine();
            content.showText("Title: " + complaint.getTitle());
            content.newLine();
            content.showText("Description: " + complaint.getDescription());
            content.newLine();
            content.showText("Priority: " + (complaint.getPriority() != null ? complaint.getPriority().name() : "N/A"));
            content.newLine();
            content.showText("Status: " + (complaint.getStatus() != null ? complaint.getStatus().name() : "N/A"));
            content.newLine();
            content.showText("Department: " + (complaint.getAssignedTo() != null && complaint.getAssignedTo().getDepartment() != null
                    ? complaint.getAssignedTo().getDepartment().getName() : "N/A"));
            content.newLine();
            content.showText("Reported by: " + (complaint.getUser() != null ? complaint.getUser().getFullName() : "Unknown"));
            content.endText();
            content.close();

            doc.save(file);
        }
    }

    // ---------------- CSV generation ----------------
    private void generateCsv(Complaint complaint, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath());
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("ID", "Title", "Description", "Priority", "Status", "Department", "ReportedBy"))) {

            csvPrinter.printRecord(
                    "R" + complaint.getId(),
                    complaint.getTitle(),
                    complaint.getDescription(),
                    complaint.getPriority() != null ? complaint.getPriority().name() : "N/A",
                    complaint.getStatus() != null ? complaint.getStatus().name() : "N/A",
                    complaint.getAssignedTo() != null && complaint.getAssignedTo().getDepartment() != null
                            ? complaint.getAssignedTo().getDepartment().getName() : "N/A",
                    complaint.getUser() != null ? complaint.getUser().getFullName() : "Unknown"
            );

            csvPrinter.flush();
        }
    }

    // ---------------- Download endpoint ----------------
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String fileName) throws IOException {
        Path file = exportDir.resolve(fileName);
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }
}
