package com.carepath.service;

import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.AccessStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.PatientClinicianAccessRepository;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service("accessControlService")
public class AccessControlService {

    private static final Logger log = LoggerFactory.getLogger(AccessControlService.class);

    private final PatientRepository patientRepository;
    private final ClinicianProfileRepository clinicianProfileRepository;
    private final PatientClinicianAccessRepository patientClinicianAccessRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public AccessControlService(PatientRepository patientRepository,
                                ClinicianProfileRepository clinicianProfileRepository,
                                PatientClinicianAccessRepository patientClinicianAccessRepository,
                                UserRepository userRepository,
                                AuditLogService auditLogService) {
        this.patientRepository = patientRepository;
        this.clinicianProfileRepository = clinicianProfileRepository;
        this.patientClinicianAccessRepository = patientClinicianAccessRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Evaluates whether the given principal has authorization to access the target patient.
     * Rules:
     * 1. ADMIN has global operational administrative access.
     * 2. PATIENT can access only their own record.
     * 3. CLINICIAN can access ONLY explicitly assigned patients with ACTIVE access status.
     */
    @Transactional(readOnly = true)
    public boolean canAccessPatient(UserPrincipal principal, UUID patientId) {
        if (principal == null || patientId == null) {
            return false;
        }

        // 1. Admin access
        if (isAdmin(principal)) {
            return true;
        }

        Optional<PatientProfile> patientOpt = patientRepository.findById(patientId);
        if (patientOpt.isEmpty()) {
            return false;
        }
        PatientProfile patient = patientOpt.get();

        // 2. Patient ownership access
        if (isPatientOwner(principal, patient)) {
            return true;
        }

        // 3. Clinician delegated relationship access
        if (isClinician(principal)) {
            return hasActiveClinicianAssignment(principal.getId(), patient.getId());
        }

        return false;
    }

    /**
     * Enforces patient access and records an audit log on violation.
     */
    @Transactional(readOnly = true)
    public PatientProfile checkAndGetPatient(UUID patientId, UserPrincipal principal, String operation, String clientIp) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to access patient resources");
        }

        PatientProfile patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found with ID: " + patientId));

        if (!canAccessPatient(principal, patientId)) {
            log.warn("[ACCESS_DENIED] Principal {} ({}) denied for patient {} on op '{}'",
                    principal.getId(), principal.getUsername(), patientId, operation);

            User actor = userRepository.findById(principal.getId()).orElse(null);
            auditLogService.recordEvent(
                    actor,
                    principal.getUsername(),
                    patientId,
                    "FORBIDDEN_ACCESS_ATTEMPT",
                    "PatientProfile",
                    patientId,
                    "FORBIDDEN",
                    clientIp,
                    null,
                    Map.of("operation", operation, "reason", "Insufficient privileges or missing clinician assignment")
            );

            throw new AccessDeniedException("Access denied: You are not authorized to access patient " + patientId);
        }

        return patient;
    }

    /**
     * Enforces user ownership (e.g. for notifications, preferences, user accounts).
     */
    public void checkUserOwnership(UUID targetUserId, UserPrincipal principal, String resourceType, String clientIp) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }

        boolean isSelf = principal.getId().equals(targetUserId);
        boolean isAdmin = isAdmin(principal);

        if (!isSelf && !isAdmin) {
            log.warn("[OWNERSHIP_DENIED] Principal {} ({}) denied for user-owned resource {} ({})",
                    principal.getId(), principal.getUsername(), targetUserId, resourceType);

            User actor = userRepository.findById(principal.getId()).orElse(null);
            auditLogService.recordEvent(
                    actor,
                    principal.getUsername(),
                    null,
                    "FORBIDDEN_ACCESS_ATTEMPT",
                    resourceType,
                    targetUserId,
                    "FORBIDDEN",
                    clientIp,
                    null,
                    Map.of("targetUserId", targetUserId.toString(), "reason", "Resource ownership violation")
            );

            throw new AccessDeniedException("Access denied: You do not own this resource");
        }
    }

    public boolean isAdmin(UserPrincipal principal) {
        return principal.getRole() == Role.ROLE_ADMIN ||
                principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public boolean isClinician(UserPrincipal principal) {
        return principal.getRole() == Role.ROLE_CLINICIAN ||
                principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLINICIAN"));
    }

    public boolean isPatientOwner(UserPrincipal principal, PatientProfile patient) {
        return patient.getUser() != null && patient.getUser().getId().equals(principal.getId());
    }

    public boolean hasActiveClinicianAssignment(UUID clinicianUserId, UUID patientProfileId) {
        Optional<ClinicianProfile> clinicianOpt = clinicianProfileRepository.findByUserId(clinicianUserId);
        if (clinicianOpt.isEmpty()) {
            return false;
        }

        return patientClinicianAccessRepository
                .findByClinicianIdAndPatientIdAndAccessStatus(
                        clinicianOpt.get().getId(), patientProfileId, AccessStatus.ACTIVE
                )
                .isPresent();
    }
}
