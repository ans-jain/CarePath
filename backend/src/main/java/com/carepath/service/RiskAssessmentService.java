package com.carepath.service;

import com.carepath.api.dto.RiskAssessmentRecordDTO;
import com.carepath.api.dto.RiskAssessmentResponseDTO;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.Role;
import com.carepath.domain.events.RiskAssessmentCompletedEvent;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.RiskAssessment;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.RiskAssessmentRepository;
import com.carepath.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;

    @Autowired
    public RiskAssessmentService(RiskAssessmentRepository riskAssessmentRepository,
                                 PatientRepository patientRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 AccessControlService accessControlService,
                                 AuditLogService auditLogService) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.patientRepository = patientRepository;
        this.eventPublisher = eventPublisher;
        this.accessControlService = accessControlService;
        this.auditLogService = auditLogService;
    }

    public RiskAssessmentService(RiskAssessmentRepository riskAssessmentRepository,
                                 PatientRepository patientRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this(riskAssessmentRepository, patientRepository, eventPublisher, null, null);
    }

    @Transactional
    public RiskAssessmentResponseDTO recordCompletedAssessment(
            UUID patientId, RiskAssessmentRecordDTO dto, UserPrincipal principal) {

        PatientProfile patient = resolvePatient(patientId, principal);

        RiskAssessment assessment = new RiskAssessment(
                patient,
                Instant.now(),
                dto.getModelVersion(),
                dto.getOverallRiskScore(),
                dto.getRiskCategory(),
                dto.getConfidenceLevel(),
                dto.getFeatureSnapshot() != null ? dto.getFeatureSnapshot() : "{}"
        );

        RiskAssessment saved = riskAssessmentRepository.save(assessment);
        log.info("[ASSESSMENT_RECORDED] Saved RiskAssessment ID: {}, Patient ID: {}, Tier: {}, Score: {}",
                saved.getId(), patient.getId(), saved.getRiskCategory(), saved.getOverallRiskScore());

        // Publish event to trigger decoupled asynchronous notifications
        eventPublisher.publishEvent(new RiskAssessmentCompletedEvent(
                patient.getUser().getId(),
                patient.getId(),
                saved.getId(),
                saved.getRiskCategory().name(),
                saved.getOverallRiskScore(),
                "assessment-" + saved.getId()
        ));
        log.info("[ASSESSMENT_EVENT_PUBLISHED] Published RiskAssessmentCompletedEvent for assessment ID: {}", saved.getId());

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    patient.getUser(),
                    principal.getUsername(),
                    patient.getId(),
                    "ASSESSMENT_CREATED",
                    "RiskAssessment",
                    saved.getId(),
                    "SUCCESS",
                    "127.0.0.1",
                    null,
                    Map.of(
                            "tier", saved.getRiskCategory().name(),
                            "modelVersion", saved.getModelVersion()
                    )
            );
        }

        return RiskAssessmentResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public RiskAssessmentResponseDTO getLatestAssessment(UUID patientId, UserPrincipal principal) {
        PatientProfile patient = resolvePatient(patientId, principal);
        RiskAssessment assessment = riskAssessmentRepository.findFirstByPatientIdOrderByAssessmentTimestampDesc(patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No risk assessment found for patient: " + patient.getId()));

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    patient.getUser(),
                    principal.getUsername(),
                    patient.getId(),
                    "ASSESSMENT_VIEWED",
                    "RiskAssessment",
                    assessment.getId(),
                    "SUCCESS",
                    "127.0.0.1",
                    null,
                    Map.of("tier", assessment.getRiskCategory().name())
            );
        }

        return RiskAssessmentResponseDTO.fromEntity(assessment);
    }

    @Transactional(readOnly = true)
    public Page<RiskAssessmentResponseDTO> getAssessmentHistory(
            UUID patientId, Pageable pageable, UserPrincipal principal) {
        PatientProfile patient = resolvePatient(patientId, principal);
        return riskAssessmentRepository.findByPatientIdOrderByAssessmentTimestampDesc(patient.getId(), pageable)
                .map(RiskAssessmentResponseDTO::fromEntity);
    }

    private PatientProfile resolvePatient(UUID patientId, UserPrincipal principal) {
        if (patientId == null) {
            return patientRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for authenticated user: " + principal.getId()));
        }

        if (accessControlService != null) {
            return accessControlService.checkAndGetPatient(patientId, principal, "RISK_ASSESSMENT", "127.0.0.1");
        }

        PatientProfile patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + patientId));

        // Fallback for tests without AccessControlService
        boolean isOwner = patient.getUser().getId().equals(principal.getId());
        boolean isStaff = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ROLE_CLINICIAN.name()) ||
                               a.getAuthority().equals("ROLE_" + Role.ROLE_ADMIN.name()));

        if (!isOwner && !isStaff) {
            throw new AccessDeniedException("Access denied: You do not have permission to access patient profile: " + patientId);
        }

        return patient;
    }
}
