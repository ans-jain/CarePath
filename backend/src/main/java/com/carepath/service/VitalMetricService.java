package com.carepath.service;

import com.carepath.api.dto.*;
import com.carepath.api.exception.InvalidCredentialsException;
import com.carepath.api.exception.InvalidMeasurementException;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.carepath.domain.enums.MetricType;
import com.carepath.domain.events.VitalMetricLoggedEvent;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.models.VitalMetric;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.VitalRepository;
import com.carepath.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class VitalMetricService {

    private static final Logger log = LoggerFactory.getLogger(VitalMetricService.class);

    private final VitalRepository vitalRepository;
    private final PatientRepository patientRepository;
    private final AuditLogRepository auditLogRepository;
    private final PhysiologicalRangeValidator rangeValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;

    @Autowired
    public VitalMetricService(VitalRepository vitalRepository,
                              PatientRepository patientRepository,
                              AuditLogRepository auditLogRepository,
                              PhysiologicalRangeValidator rangeValidator,
                              ApplicationEventPublisher eventPublisher,
                              AccessControlService accessControlService,
                              AuditLogService auditLogService) {
        this.vitalRepository = vitalRepository;
        this.patientRepository = patientRepository;
        this.auditLogRepository = auditLogRepository;
        this.rangeValidator = rangeValidator;
        this.eventPublisher = eventPublisher;
        this.accessControlService = accessControlService;
        this.auditLogService = auditLogService;
    }

    public VitalMetricService(VitalRepository vitalRepository,
                              PatientRepository patientRepository,
                              AuditLogRepository auditLogRepository,
                              PhysiologicalRangeValidator rangeValidator,
                              ApplicationEventPublisher eventPublisher) {
        this(vitalRepository, patientRepository, auditLogRepository, rangeValidator, eventPublisher, null, null);
    }

    @Transactional
    public VitalResponseDTO recordVital(CreateVitalRequestDTO request,
                                        UUID routePatientId,
                                        UserPrincipal principal,
                                        String clientIp) {
        PatientProfile profile = resolvePatient(routePatientId, principal);

        if (request.getMetricType() == null) {
            throw new InvalidMeasurementException("Metric type is required");
        }
        if (request.getValue() == null) {
            throw new InvalidMeasurementException("Measurement value is required");
        }

        // Validate physiological bounds
        rangeValidator.validate(request.getMetricType(), request.getValue());

        Instant recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now();
        if (recordedAt.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw new InvalidMeasurementException("Vital measurement timestamp cannot be in the future");
        }

        String unit = (request.getUnit() != null && !request.getUnit().trim().isEmpty())
                ? request.getUnit().trim()
                : rangeValidator.getCanonicalUnit(request.getMetricType());

        MeasurementContext context = request.getMeasurementContext() != null
                ? request.getMeasurementContext()
                : MeasurementContext.RESTING;

        MeasurementSource source = request.getSource() != null
                ? request.getSource()
                : MeasurementSource.MANUAL;

        VitalMetric vital = new VitalMetric(
                profile,
                recordedAt,
                request.getMetricType(),
                request.getValue(),
                unit,
                context,
                source
        );

        VitalMetric saved = vitalRepository.save(vital);

        // Publish decoupled event for downstream analytics
        eventPublisher.publishEvent(new VitalMetricLoggedEvent(
                saved.getId(),
                profile.getId(),
                saved.getMetricType(),
                saved.getValue(),
                saved.getUnit(),
                saved.getRecordedAt()
        ));

        // Audit logging
        recordAuditLog(profile.getUser(), profile.getId(), "RECORD_VITAL", "VitalMetric", saved.getId(), clientIp);

        return mapToResponse(saved);
    }

    @Transactional
    public List<VitalResponseDTO> recordBatchVitals(BatchCreateVitalRequestDTO request,
                                                    UUID routePatientId,
                                                    UserPrincipal principal,
                                                    String clientIp) {
        PatientProfile profile = resolvePatient(routePatientId, principal);

        if (request.getEntries() == null || request.getEntries().isEmpty()) {
            throw new InvalidMeasurementException("Batch entries list cannot be empty");
        }

        List<VitalMetric> vitalsToSave = new ArrayList<>(request.getEntries().size());

        for (BatchVitalEntryDTO entry : request.getEntries()) {
            if (entry.getMetricType() == null) {
                throw new InvalidMeasurementException("Metric type is required for all entries");
            }
            if (entry.getValue() == null) {
                throw new InvalidMeasurementException("Measurement value is required for all entries");
            }

            rangeValidator.validate(entry.getMetricType(), entry.getValue());

            Instant recordedAt = entry.getRecordedAt() != null
                    ? entry.getRecordedAt()
                    : (request.getRecordedAt() != null ? request.getRecordedAt() : Instant.now());

            if (recordedAt.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
                throw new InvalidMeasurementException("Vital measurement timestamp cannot be in the future");
            }

            String unit = (entry.getUnit() != null && !entry.getUnit().trim().isEmpty())
                    ? entry.getUnit().trim()
                    : rangeValidator.getCanonicalUnit(entry.getMetricType());

            MeasurementContext context = entry.getMeasurementContext() != null
                    ? entry.getMeasurementContext()
                    : (request.getDefaultContext() != null ? request.getDefaultContext() : MeasurementContext.RESTING);

            MeasurementSource source = entry.getSource() != null
                    ? entry.getSource()
                    : (request.getDefaultSource() != null ? request.getDefaultSource() : MeasurementSource.MANUAL);

            vitalsToSave.add(new VitalMetric(
                    profile,
                    recordedAt,
                    entry.getMetricType(),
                    entry.getValue(),
                    unit,
                    context,
                    source
            ));
        }

        List<VitalMetric> savedVitals = vitalRepository.saveAll(vitalsToSave);

        // Publish events for each saved vital
        for (VitalMetric saved : savedVitals) {
            eventPublisher.publishEvent(new VitalMetricLoggedEvent(
                    saved.getId(),
                    profile.getId(),
                    saved.getMetricType(),
                    saved.getValue(),
                    saved.getUnit(),
                    saved.getRecordedAt()
            ));
        }

        // Single audit log for batch ingestion
        recordAuditLog(profile.getUser(), profile.getId(), "BATCH_RECORD_VITALS", "VitalMetric", null, clientIp);

        return savedVitals.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public VitalPageResponseDTO getVitals(UUID routePatientId,
                                          MetricType metricType,
                                          Instant startDate,
                                          Instant endDate,
                                          Pageable pageable,
                                          UserPrincipal principal) {
        PatientProfile profile = resolvePatient(routePatientId, principal);

        Specification<VitalMetric> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("patient").get("id"), profile.getId()));

            if (metricType != null) {
                predicates.add(cb.equal(root.get("metricType"), metricType));
            }
            if (startDate != null && endDate != null) {
                predicates.add(cb.between(root.get("recordedAt"), startDate, endDate));
            } else if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("recordedAt"), startDate));
            } else if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("recordedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<VitalMetric> pageResult = vitalRepository.findAll(spec, pageable);

        List<VitalResponseDTO> dtoList = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new VitalPageResponseDTO(
                dtoList,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isFirst(),
                pageResult.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<VitalResponseDTO> getLatestVitals(UUID routePatientId,
                                                  MetricType metricType,
                                                  UserPrincipal principal) {
        PatientProfile profile = resolvePatient(routePatientId, principal);

        List<VitalMetric> vitals;
        if (metricType != null) {
            vitals = vitalRepository.findFirstByPatientIdAndMetricTypeOrderByRecordedAtDesc(profile.getId(), metricType)
                    .map(List::of)
                    .orElse(Collections.emptyList());
        } else {
            vitals = vitalRepository.findLatestPerMetricType(profile.getId());
        }

        return vitals.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public VitalResponseDTO getVitalById(UUID id, UUID routePatientId, UserPrincipal principal) {
        VitalMetric vital = vitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vital measurement not found with ID: " + id));

        PatientProfile profile = (routePatientId != null)
                ? resolvePatient(routePatientId, principal)
                : vital.getPatient();

        verifyOwnership(profile, principal);

        // Verify the vital belongs to the requested/authorized patient
        if (!vital.getPatient().getId().equals(profile.getId())) {
            log.warn("Cross-patient vital access violation: vital {} belongs to patient {}, requested by patient {}",
                    id, vital.getPatient().getId(), profile.getId());
            throw new AccessDeniedException("Access denied: Measurement does not belong to authorized patient");
        }

        return mapToResponse(vital);
    }

    private PatientProfile resolvePatient(UUID routePatientId, UserPrincipal principal) {
        validatePrincipal(principal);
        if (routePatientId != null) {
            if (accessControlService != null) {
                return accessControlService.checkAndGetPatient(routePatientId, principal, "VITALS_ACCESS", "127.0.0.1");
            }
            PatientProfile profile = patientRepository.findById(routePatientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + routePatientId));
            verifyOwnership(profile, principal);
            return profile;
        } else {
            return patientRepository.findByUserId(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found. Please complete intake profile."));
        }
    }

    private void verifyOwnership(PatientProfile profile, UserPrincipal principal) {
        if (accessControlService != null) {
            if (!accessControlService.canAccessPatient(principal, profile.getId())) {
                log.warn("Ownership violation attempt: user {} attempted to access patient {}", principal.getId(), profile.getId());
                throw new AccessDeniedException("Access denied: You do not have permission to access vitals for this patient");
            }
            return;
        }

        boolean isOwner = profile.getUser() != null && profile.getUser().getId().equals(principal.getId());
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            log.warn("Ownership violation attempt: user {} attempted to access patient {}", principal.getId(), profile.getId());
            throw new AccessDeniedException("Access denied: You do not have permission to access vitals for this patient");
        }
    }

    private void validatePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Authentication required");
        }
    }

    private VitalResponseDTO mapToResponse(VitalMetric vital) {
        return new VitalResponseDTO(
                vital.getId(),
                vital.getPatient().getId(),
                vital.getMetricType(),
                vital.getValue(),
                vital.getUnit(),
                vital.getRecordedAt(),
                vital.getMeasurementContext(),
                vital.getSource(),
                vital.getCreatedAt()
        );
    }

    private void recordAuditLog(User actor, UUID targetPatientId, String actionType,
                                String entityName, UUID entityId, String clientIp) {
        if (auditLogService != null) {
            auditLogService.recordEvent(
                    actor,
                    actor != null ? actor.getEmail() : null,
                    targetPatientId,
                    actionType,
                    entityName,
                    entityId,
                    "SUCCESS",
                    clientIp != null ? clientIp : "127.0.0.1",
                    null,
                    Map.of("status", "SUCCESS")
            );
            return;
        }

        try {
            AuditLog auditLog = new AuditLog(
                    actor,
                    targetPatientId,
                    actionType,
                    entityName,
                    entityId,
                    clientIp != null ? clientIp : "127.0.0.1",
                    "{\"status\":\"SUCCESS\"}"
            );
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to persist audit log: {}", e.getMessage());
        }
    }
}
