package com.carepath.service;

import com.carepath.api.dto.*;
import com.carepath.api.exception.InvalidCredentialsException;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.AlcoholUse;
import com.carepath.domain.enums.SmokingStatus;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
public class PatientProfileService {

    private static final Logger log = LoggerFactory.getLogger(PatientProfileService.class);

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;

    @Autowired
    public PatientProfileService(PatientRepository patientRepository,
                                 UserRepository userRepository,
                                 AuditLogRepository auditLogRepository,
                                 ObjectMapper objectMapper,
                                 AccessControlService accessControlService,
                                 AuditLogService auditLogService) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.accessControlService = accessControlService;
        this.auditLogService = auditLogService;
    }

    public PatientProfileService(PatientRepository patientRepository,
                                 UserRepository userRepository,
                                 AuditLogRepository auditLogRepository,
                                 ObjectMapper objectMapper) {
        this(patientRepository, userRepository, auditLogRepository, objectMapper, null, null);
    }

    public static BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null ||
                heightCm.compareTo(BigDecimal.ZERO) <= 0 || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        // BMI = weightKg / (heightM)^2
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal heightM2 = heightM.multiply(heightM);
        return weightKg.divide(heightM2, 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public PatientProfileResponseDTO getProfileForCurrentUser(UserPrincipal principal) {
        validatePrincipal(principal);
        PatientProfile profile = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found. Please complete intake profile."));

        return mapToProfileResponse(profile, profile.getUser());
    }

    @Transactional(readOnly = true)
    public PatientProfileResponseDTO getProfileById(UUID patientId, UserPrincipal principal) {
        validatePrincipal(principal);
        PatientProfile profile = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + patientId));

        verifyOwnership(profile, principal);

        return mapToProfileResponse(profile, profile.getUser());
    }

    @Transactional
    public PatientProfileResponseDTO upsertProfileForCurrentUser(PatientProfileRequestDTO request,
                                                                UserPrincipal principal,
                                                                String clientIp) {
        validatePrincipal(principal);
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        PatientProfile profile = patientRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    PatientProfile newProfile = new PatientProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        applyProfileUpdates(profile, request);
        PatientProfile savedProfile = patientRepository.save(profile);

        // Optionally update demographic contact fields in User entity if provided
        boolean userUpdated = false;
        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName().trim());
            userUpdated = true;
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName().trim());
            userUpdated = true;
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
            userUpdated = true;
        }
        if (userUpdated) {
            userRepository.save(user);
        }

        recordAuditLog(user, savedProfile.getId(), "PROFILE_UPSERT", "PatientProfile", savedProfile.getId(), clientIp);

        return mapToProfileResponse(savedProfile, user);
    }

    @Transactional
    public PatientProfileResponseDTO updateProfileById(UUID patientId,
                                                      PatientProfileRequestDTO request,
                                                      UserPrincipal principal,
                                                      String clientIp) {
        validatePrincipal(principal);
        PatientProfile profile = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + patientId));

        verifyOwnership(profile, principal);

        applyProfileUpdates(profile, request);
        PatientProfile savedProfile = patientRepository.save(profile);

        recordAuditLog(profile.getUser(), savedProfile.getId(), "PROFILE_UPDATE", "PatientProfile", savedProfile.getId(), clientIp);

        return mapToProfileResponse(savedProfile, profile.getUser());
    }

    @Transactional
    public PatientIntakeResponseDTO submitIntake(PatientIntakeRequestDTO request,
                                                UserPrincipal principal,
                                                String clientIp) {
        validatePrincipal(principal);
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        PatientProfile profile = patientRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    PatientProfile newProfile = new PatientProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setBiologicalSex(request.getBiologicalSex());
        profile.setHeightCm(request.getHeightCm());
        profile.setBaselineWeightKg(request.getBaselineWeightKg());
        profile.setSmokingStatus(request.getSmokingStatus() != null ? request.getSmokingStatus() : SmokingStatus.NEVER);
        profile.setAlcoholUse(request.getAlcoholUse() != null ? request.getAlcoholUse() : AlcoholUse.NONE);
        profile.setMedicalHistory(serializeMedicalHistory(request.getMedicalHistory()));

        PatientProfile savedProfile = patientRepository.save(profile);

        recordAuditLog(user, savedProfile.getId(), "INTAKE_SUBMISSION", "PatientProfile", savedProfile.getId(), clientIp);

        BigDecimal bmi = calculateBmi(savedProfile.getHeightCm(), savedProfile.getBaselineWeightKg());

        return new PatientIntakeResponseDTO(
                savedProfile.getId(),
                user.getId(),
                savedProfile.getDateOfBirth(),
                savedProfile.getBiologicalSex(),
                savedProfile.getHeightCm(),
                savedProfile.getBaselineWeightKg(),
                bmi,
                savedProfile.getSmokingStatus(),
                savedProfile.getAlcoholUse(),
                deserializeMedicalHistory(savedProfile.getMedicalHistory()),
                savedProfile.getUpdatedAt(),
                "Intake completed successfully. Personal baseline ready for tracking."
        );
    }

    @Transactional(readOnly = true)
    public PatientIntakeResponseDTO getIntakeForCurrentUser(UserPrincipal principal) {
        validatePrincipal(principal);
        PatientProfile profile = patientRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient intake has not been completed yet. Please complete intake profile."));

        BigDecimal bmi = calculateBmi(profile.getHeightCm(), profile.getBaselineWeightKg());

        return new PatientIntakeResponseDTO(
                profile.getId(),
                profile.getUser().getId(),
                profile.getDateOfBirth(),
                profile.getBiologicalSex(),
                profile.getHeightCm(),
                profile.getBaselineWeightKg(),
                bmi,
                profile.getSmokingStatus(),
                profile.getAlcoholUse(),
                deserializeMedicalHistory(profile.getMedicalHistory()),
                profile.getUpdatedAt(),
                "Intake profile retrieved successfully."
        );
    }

    private void applyProfileUpdates(PatientProfile profile, PatientProfileRequestDTO request) {
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setBiologicalSex(request.getBiologicalSex());
        profile.setHeightCm(request.getHeightCm());
        profile.setBaselineWeightKg(request.getBaselineWeightKg());
        if (request.getSmokingStatus() != null) {
            profile.setSmokingStatus(request.getSmokingStatus());
        }
        if (request.getAlcoholUse() != null) {
            profile.setAlcoholUse(request.getAlcoholUse());
        }
        if (request.getMedicalHistory() != null) {
            profile.setMedicalHistory(serializeMedicalHistory(request.getMedicalHistory()));
        }
    }

    private void verifyOwnership(PatientProfile profile, UserPrincipal principal) {
        if (accessControlService != null) {
            if (!accessControlService.canAccessPatient(principal, profile.getId())) {
                log.warn("Ownership violation attempt: user {} attempted to access patient {}", principal.getId(), profile.getId());
                throw new AccessDeniedException("Access denied: You do not have permission to access this patient profile");
            }
            return;
        }

        boolean isOwner = profile.getUser().getId().equals(principal.getId());
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            log.warn("Ownership violation attempt: user {} attempted to access patient {}", principal.getId(), profile.getId());
            throw new AccessDeniedException("Access denied: You do not have permission to access this patient profile");
        }
    }

    private void validatePrincipal(UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Authentication required");
        }
    }

    private PatientProfileResponseDTO mapToProfileResponse(PatientProfile profile, User user) {
        BigDecimal bmi = calculateBmi(profile.getHeightCm(), profile.getBaselineWeightKg());
        return new PatientProfileResponseDTO(
                profile.getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                profile.getDateOfBirth(),
                profile.getBiologicalSex(),
                profile.getHeightCm(),
                profile.getBaselineWeightKg(),
                bmi,
                profile.getSmokingStatus(),
                profile.getAlcoholUse(),
                deserializeMedicalHistory(profile.getMedicalHistory()),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String serializeMedicalHistory(Map<String, Object> history) {
        if (history == null || history.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(history);
        } catch (Exception e) {
            log.warn("Failed to serialize medical history to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Object> deserializeMedicalHistory(String json) {
        if (json == null || json.trim().isEmpty() || json.trim().equals("{}")) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize medical history JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
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
