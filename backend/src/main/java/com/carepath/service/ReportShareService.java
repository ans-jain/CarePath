package com.carepath.service;

import com.carepath.api.dto.DoctorDirectoryItemDTO;
import com.carepath.api.dto.ReportShareRequestDTO;
import com.carepath.api.dto.ReportShareResponseDTO;
import com.carepath.api.dto.SharedReportItemDTO;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.ReportShareStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.ReportShare;
import com.carepath.domain.models.RiskAssessment;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.ReportShareRepository;
import com.carepath.domain.repository.RiskAssessmentRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportShareService {

    private static final Logger log = LoggerFactory.getLogger(ReportShareService.class);

    private final ReportShareRepository reportShareRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final UserRepository userRepository;
    private final ClinicianProfileRepository clinicianProfileRepository;
    private final AuditLogService auditLogService;

    public ReportShareService(ReportShareRepository reportShareRepository,
                              RiskAssessmentRepository riskAssessmentRepository,
                              UserRepository userRepository,
                              ClinicianProfileRepository clinicianProfileRepository,
                              @Autowired(required = false) AuditLogService auditLogService) {
        this.reportShareRepository = reportShareRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.userRepository = userRepository;
        this.clinicianProfileRepository = clinicianProfileRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Returns all active, verified doctors registered in the system.
     */
    @Transactional(readOnly = true)
    public List<DoctorDirectoryItemDTO> getVerifiedDoctors() {
        List<User> doctors = userRepository.findByRoleAndStatus(Role.ROLE_CLINICIAN, AccountStatus.ACTIVE);
        return doctors.stream()
                .sorted(Comparator.comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> {
                    ClinicianProfile profile = clinicianProfileRepository != null
                            ? clinicianProfileRepository.findByUserId(user.getId()).orElse(null)
                            : null;
                    return DoctorDirectoryItemDTO.fromUserAndProfile(user, profile);
                })
                .collect(Collectors.toList());
    }

    /**
     * Shares a specific assessment report with a verified doctor.
     * Enforces that the caller is the patient who owns the report.
     */
    @Transactional
    public ReportShareResponseDTO shareReport(ReportShareRequestDTO request, UserPrincipal principal, String clientIp) {
        RiskAssessment report = riskAssessmentRepository.findById(request.getReportId())
                .orElseThrow(() -> new ResourceNotFoundException("Risk assessment report not found with ID: " + request.getReportId()));

        // Enforce ownership: patient can only share their own report
        UUID ownerUserId = report.getPatient().getUser().getId();
        if (!ownerUserId.equals(principal.getId())) {
            throw new AccessDeniedException("Access denied: You are only permitted to share your own assessment reports.");
        }

        // Validate target doctor exists and is active/verified
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Target doctor not found with ID: " + request.getDoctorId()));

        if (doctor.getRole() != Role.ROLE_CLINICIAN || doctor.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Selected healthcare provider is not an active, verified doctor.");
        }

        // Create or update share record
        ReportShare share = reportShareRepository.findByReportIdAndDoctorId(report.getId(), doctor.getId())
                .orElseGet(() -> new ReportShare(report, report.getPatient(), doctor));

        share.setStatus(ReportShareStatus.NEW);
        share.setSharedAt(Instant.now());
        share.setRevokedAt(null);
        share.setViewedAt(null);

        ReportShare saved = reportShareRepository.save(share);

        log.info("[REPORT_SHARED] Report ID '{}' shared with Doctor '{}' (ID: {}) by Patient '{}' (ID: {})",
                report.getId(), doctor.getEmail(), doctor.getId(), principal.getUsername(), report.getPatient().getId());

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    report.getPatient().getUser(),
                    principal.getUsername(),
                    report.getPatient().getId(),
                    "REPORT_SHARED",
                    "ReportShare",
                    saved.getId(),
                    "SUCCESS",
                    clientIp != null ? clientIp : "127.0.0.1",
                    null,
                    Map.of(
                            "reportId", report.getId().toString(),
                            "doctorId", doctor.getId().toString(),
                            "doctorEmail", doctor.getEmail(),
                            "patientId", report.getPatient().getId().toString()
                    )
            );
        }

        ClinicianProfile profile = clinicianProfileRepository != null
                ? clinicianProfileRepository.findByUserId(doctor.getId()).orElse(null)
                : null;

        return ReportShareResponseDTO.fromEntity(saved, profile);
    }

    /**
     * Returns all share records for a report (to show patient who has access).
     * Enforces patient ownership of the report.
     */
    @Transactional(readOnly = true)
    public List<ReportShareResponseDTO> getSharesForReport(UUID reportId, UserPrincipal principal) {
        RiskAssessment report = riskAssessmentRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk assessment report not found with ID: " + reportId));

        UUID ownerUserId = report.getPatient().getUser().getId();
        if (!ownerUserId.equals(principal.getId())) {
            throw new AccessDeniedException("Access denied: You can only view sharing status for your own reports.");
        }

        List<ReportShare> shares = reportShareRepository.findByReportId(reportId);
        return shares.stream()
                .map(share -> {
                    ClinicianProfile profile = clinicianProfileRepository != null
                            ? clinicianProfileRepository.findByUserId(share.getDoctor().getId()).orElse(null)
                            : null;
                    return ReportShareResponseDTO.fromEntity(share, profile);
                })
                .collect(Collectors.toList());
    }

    /**
     * Revokes access to a shared report.
     * Enforces that only the patient who owns the report can revoke it.
     */
    @Transactional
    public ReportShareResponseDTO revokeShare(UUID shareId, UserPrincipal principal, String clientIp) {
        ReportShare share = reportShareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Report share record not found with ID: " + shareId));

        UUID ownerUserId = share.getPatient().getUser().getId();
        if (!ownerUserId.equals(principal.getId())) {
            throw new AccessDeniedException("Access denied: You can only revoke sharing access for your own reports.");
        }

        share.setStatus(ReportShareStatus.REVOKED);
        share.setRevokedAt(Instant.now());
        ReportShare saved = reportShareRepository.save(share);

        log.info("[REPORT_SHARE_REVOKED] Revoked access to Report ID '{}' for Doctor ID '{}' by Patient '{}'",
                share.getReport().getId(), share.getDoctor().getId(), principal.getUsername());

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    share.getPatient().getUser(),
                    principal.getUsername(),
                    share.getPatient().getId(),
                    "REPORT_SHARE_REVOKED",
                    "ReportShare",
                    saved.getId(),
                    "SUCCESS",
                    clientIp != null ? clientIp : "127.0.0.1",
                    null,
                    Map.of(
                            "reportId", share.getReport().getId().toString(),
                            "doctorId", share.getDoctor().getId().toString(),
                            "status", ReportShareStatus.REVOKED.name()
                    )
            );
        }

        ClinicianProfile profile = clinicianProfileRepository != null
                ? clinicianProfileRepository.findByUserId(share.getDoctor().getId()).orElse(null)
                : null;

        return ReportShareResponseDTO.fromEntity(saved, profile);
    }

    /**
     * Returns only reports that have been actively shared with the authenticated doctor.
     * Scoped strictly to the calling doctor and status != REVOKED.
     */
    @Transactional(readOnly = true)
    public List<SharedReportItemDTO> getSharedReportsForDoctor(UserPrincipal principal) {
        List<ReportShare> shares = reportShareRepository.findByDoctorIdAndStatusNotOrderBySharedAtDesc(
                principal.getId(), ReportShareStatus.REVOKED);

        return shares.stream()
                .map(SharedReportItemDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Doctor views a single shared report.
     * Enforces active ReportShare existence; marks status as VIEWED on first access.
     */
    @Transactional
    public SharedReportItemDTO getSharedReportDetailForDoctor(UUID reportId, UserPrincipal principal, String clientIp) {
        ReportShare share = reportShareRepository.findActiveShareForDoctorAndReport(
                principal.getId(), reportId, ReportShareStatus.REVOKED)
                .orElseThrow(() -> new AccessDeniedException("You do not have permission to view this report. Access may not have been granted or was revoked by the patient."));

        if (share.getStatus() == ReportShareStatus.NEW) {
            share.setStatus(ReportShareStatus.VIEWED);
            share.setViewedAt(Instant.now());
            share = reportShareRepository.save(share);
        }

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    share.getDoctor(),
                    principal.getUsername(),
                    share.getPatient().getId(),
                    "SHARED_REPORT_VIEWED",
                    "RiskAssessment",
                    share.getReport().getId(),
                    "SUCCESS",
                    clientIp != null ? clientIp : "127.0.0.1",
                    null,
                    Map.of(
                            "reportId", share.getReport().getId().toString(),
                            "status", share.getStatus().name()
                    )
            );
        }

        return SharedReportItemDTO.fromEntity(share);
    }
}
