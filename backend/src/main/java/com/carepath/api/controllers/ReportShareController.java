package com.carepath.api.controllers;

import com.carepath.api.dto.DoctorDirectoryItemDTO;
import com.carepath.api.dto.ReportShareRequestDTO;
import com.carepath.api.dto.ReportShareResponseDTO;
import com.carepath.api.dto.SharedReportItemDTO;
import com.carepath.security.UserPrincipal;
import com.carepath.service.ReportShareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/report-shares", "/api/v1/reports/shares"})
public class ReportShareController {

    private final ReportShareService reportShareService;

    public ReportShareController(ReportShareService reportShareService) {
        this.reportShareService = reportShareService;
    }

    /**
     * Get searchable directory of verified, approved doctors in CarePath.
     * Accessible by authenticated patients.
     */
    @GetMapping({"/doctors", "/verified-doctors"})
    public ResponseEntity<List<DoctorDirectoryItemDTO>> getVerifiedDoctors() {
        List<DoctorDirectoryItemDTO> doctors = reportShareService.getVerifiedDoctors();
        return ResponseEntity.ok(doctors);
    }

    /**
     * Patient shares a specific assessment report with a verified doctor.
     */
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ReportShareResponseDTO> shareReport(
            @Valid @RequestBody ReportShareRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        ReportShareResponseDTO response = reportShareService.shareReport(requestDTO, principal, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Patient views which doctors currently have access to a specific report.
     */
    @GetMapping("/report/{reportId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<ReportShareResponseDTO>> getSharesForReport(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ReportShareResponseDTO> response = reportShareService.getSharesForReport(reportId, principal);
        return ResponseEntity.ok(response);
    }

    /**
     * Patient revokes a doctor's access to a report.
     */
    @PostMapping("/{shareId}/revoke")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ReportShareResponseDTO> revokeSharePost(
            @PathVariable UUID shareId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        ReportShareResponseDTO response = reportShareService.revokeShare(shareId, principal, clientIp);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shareId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ReportShareResponseDTO> revokeShareDelete(
            @PathVariable UUID shareId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        ReportShareResponseDTO response = reportShareService.revokeShare(shareId, principal, clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * Doctor retrieves all reports patients have shared with them.
     */
    @GetMapping({"/doctor", "/clinician"})
    @PreAuthorize("hasAnyRole('CLINICIAN', 'DOCTOR')")
    public ResponseEntity<List<SharedReportItemDTO>> getSharedReportsForDoctor(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<SharedReportItemDTO> reports = reportShareService.getSharedReportsForDoctor(principal);
        return ResponseEntity.ok(reports);
    }

    /**
     * Doctor accesses a specific shared report (enforces active ReportShare existence).
     */
    @GetMapping({"/doctor/{reportId}", "/clinician/{reportId}"})
    @PreAuthorize("hasAnyRole('CLINICIAN', 'DOCTOR')")
    public ResponseEntity<SharedReportItemDTO> getSharedReportDetailForDoctor(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        SharedReportItemDTO response = reportShareService.getSharedReportDetailForDoctor(reportId, principal, clientIp);
        return ResponseEntity.ok(response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
