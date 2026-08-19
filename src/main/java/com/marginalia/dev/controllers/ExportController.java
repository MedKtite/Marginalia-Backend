package com.marginalia.dev.controllers;

import com.marginalia.dev.security.CustomUserDetails;
import com.marginalia.dev.services.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/me")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/export/summary")
    public Map<String, Object> summary(@AuthenticationPrincipal CustomUserDetails me) {
        return exportService.getExportSummary(me.getUserId());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal CustomUserDetails me,
            @RequestParam(name = "format", defaultValue = "pdf") String format,
            @RequestParam(name = "book_id", required = false) UUID bookId) {

        if ("csv".equalsIgnoreCase(format)) {
            byte[] csvBytes = exportService.exportCsv(me.getUserId(), bookId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"marginalia-highlights.csv\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csvBytes);
        } else {
            byte[] pdfBytes = exportService.exportPdf(me.getUserId(), bookId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"marginalia-notes.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
    }

    @PostMapping("/sync")
    public Map<String, Object> triggerSync(@AuthenticationPrincipal CustomUserDetails me) {
        return Map.of(
                "status", "synced",
                "synced_at", OffsetDateTime.now().toString(),
                "user_id", me.getUserId()
        );
    }
}
