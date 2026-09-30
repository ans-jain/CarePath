package com.carepath.api.controllers;

import com.carepath.api.dto.DoctorRegistrationResponseDTO;
import com.carepath.api.exception.DoctorPendingApprovalException;
import com.carepath.api.exception.DoctorRejectedException;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/doctor", "/api/v1/clinician"})
@PreAuthorize("hasAnyRole('CLINICIAN', 'DOCTOR')")
public class DoctorController {

    private final UserRepository userRepository;
    private final ClinicianProfileRepository clinicianProfileRepository;
    private final com.carepath.service.ReportShareService reportShareService;

    public DoctorController(UserRepository userRepository,
                            ClinicianProfileRepository clinicianProfileRepository,
                            com.carepath.service.ReportShareService reportShareService) {
        this.userRepository = userRepository;
        this.clinicianProfileRepository = clinicianProfileRepository;
        this.reportShareService = reportShareService;
    }

    @GetMapping("/profile")
    public ResponseEntity<DoctorRegistrationResponseDTO> getDoctorProfile(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with ID: " + principal.getId()));

        validateDoctorStatus(user);

        ClinicianProfile profile = clinicianProfileRepository.findByUserId(user.getId()).orElse(null);
        return ResponseEntity.ok(DoctorRegistrationResponseDTO.fromUserAndProfile(user, profile));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDoctorDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with ID: " + principal.getId()));

        validateDoctorStatus(user);

        ClinicianProfile profile = clinicianProfileRepository.findByUserId(user.getId()).orElse(null);

        Map<String, Object> dashboard = Map.of(
                "status", "ACTIVE",
                "doctorName", (user.getFirstName() + " " + user.getLastName()).trim(),
                "licenseNumber", profile != null ? profile.getLicenseNumber() : "N/A",
                "specialization", profile != null ? profile.getSpecialty() : "General Practice",
                "hospitalOrganization", profile != null ? profile.getClinicName() : "CarePath Clinical Network",
                "activePatientsCount", 12,
                "highRiskAlertsCount", 3,
                "systemStatus", "HEALTHY"
        );

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/shared-reports")
    public ResponseEntity<java.util.List<com.carepath.api.dto.SharedReportItemDTO>> getSharedReports(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with ID: " + principal.getId()));
        validateDoctorStatus(user);
        return ResponseEntity.ok(reportShareService.getSharedReportsForDoctor(principal));
    }

    @GetMapping("/shared-reports/{reportId}")
    public ResponseEntity<com.carepath.api.dto.SharedReportItemDTO> getSharedReportDetail(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal UserPrincipal principal,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with ID: " + principal.getId()));
        validateDoctorStatus(user);
        String clientIp = extractClientIp(servletRequest);
        return ResponseEntity.ok(reportShareService.getSharedReportDetailForDoctor(reportId, principal, clientIp));
    }

    private String extractClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void validateDoctorStatus(User user) {
        if (user.getStatus() == AccountStatus.PENDING) {
            throw new DoctorPendingApprovalException("Your doctor registration is still pending admin approval. You will be able to access your account once an administrator approves your registration.");
        }
        if (user.getStatus() == AccountStatus.REJECTED) {
            throw new DoctorRejectedException("Your doctor registration request was not approved.");
        }
    }
}
