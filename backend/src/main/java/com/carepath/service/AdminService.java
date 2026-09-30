package com.carepath.service;

import com.carepath.api.dto.AdminUserResponseDTO;
import com.carepath.api.dto.AuditLogResponseDTO;
import com.carepath.api.dto.DoctorRegistrationResponseDTO;
import com.carepath.api.dto.SecurityOverviewDTO;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final ClinicianProfileRepository clinicianProfileRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    @Value("${app.security.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${app.security.rate-limiting.auth-limit-per-minute:30}")
    private int authRateLimitPerMinute;

    @Value("${app.security.rate-limiting.ml-limit-per-minute:60}")
    private int mlRateLimitPerMinute;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOriginsConfig;

    @Autowired
    public AdminService(UserRepository userRepository,
                        ClinicianProfileRepository clinicianProfileRepository,
                        AuditLogRepository auditLogRepository,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.clinicianProfileRepository = clinicianProfileRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
    }

    public AdminService(UserRepository userRepository,
                        AuditLogRepository auditLogRepository,
                        AuditLogService auditLogService) {
        this(userRepository, null, auditLogRepository, auditLogService);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getAuditLogs(UUID actorUserId,
                                                  String actionType,
                                                  String entityName,
                                                  String status,
                                                  Instant startDate,
                                                  Instant endDate,
                                                  Pageable pageable) {
        Page<AuditLog> page = auditLogService.queryAuditLogs(actorUserId, actionType, entityName, status, startDate, endDate, pageable);
        return page.map(AuditLogResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponseDTO::fromEntity);
    }

    @Transactional
    public AdminUserResponseDTO updateUserRole(UUID userId, Role newRole, UserPrincipal adminPrincipal, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Role previousRole = user.getRole();
        user.setRole(newRole);
        User saved = userRepository.save(user);

        log.info("[ADMIN_ROLE_CHANGE] Admin '{}' changed role for user '{}' from {} to {}",
                adminPrincipal.getUsername(), user.getEmail(), previousRole, newRole);

        User adminUser = userRepository.findById(adminPrincipal.getId()).orElse(null);
        auditLogService.recordEvent(
                adminUser,
                adminPrincipal.getUsername(),
                null,
                "ROLE_CHANGE",
                "User",
                userId,
                "SUCCESS",
                clientIp,
                null,
                Map.of(
                        "targetEmail", user.getEmail(),
                        "previousRole", previousRole.name(),
                        "newRole", newRole.name()
                )
        );

        return AdminUserResponseDTO.fromEntity(saved);
    }

    @Transactional
    public AdminUserResponseDTO updateUserStatus(UUID userId, boolean active, UserPrincipal adminPrincipal, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        boolean previousStatus = user.isActive();
        user.setActive(active);
        User saved = userRepository.save(user);

        log.info("[ADMIN_STATUS_CHANGE] Admin '{}' changed active status for user '{}' from {} to {}",
                adminPrincipal.getUsername(), user.getEmail(), previousStatus, active);

        User adminUser = userRepository.findById(adminPrincipal.getId()).orElse(null);
        auditLogService.recordEvent(
                adminUser,
                adminPrincipal.getUsername(),
                null,
                "USER_STATUS_CHANGE",
                "User",
                userId,
                "SUCCESS",
                clientIp,
                null,
                Map.of(
                        "targetEmail", user.getEmail(),
                        "previousStatus", previousStatus,
                        "newStatus", active
                )
        );

        return AdminUserResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public SecurityOverviewDTO getSecurityOverview() {
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> userCountByRole = allUsers.stream()
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        long totalAuditLogs = auditLogRepository.count();

        List<String> origins = Arrays.stream(corsAllowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> securityHeaders = List.of(
                "Content-Security-Policy (CSP)",
                "X-Content-Type-Options: nosniff",
                "X-Frame-Options: DENY",
                "Strict-Transport-Security (HSTS)",
                "Referrer-Policy: strict-origin-when-cross-origin"
        );

        return new SecurityOverviewDTO(
                rateLimitingEnabled,
                authRateLimitPerMinute,
                mlRateLimitPerMinute,
                origins,
                securityHeaders,
                userCountByRole,
                totalAuditLogs
        );
    }

    @Transactional(readOnly = true)
    public List<DoctorRegistrationResponseDTO> getDoctorRequests(String statusFilter) {
        List<User> doctorUsers;
        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                AccountStatus status = AccountStatus.valueOf(statusFilter.trim().toUpperCase());
                doctorUsers = userRepository.findByRoleAndStatus(Role.ROLE_CLINICIAN, status);
            } catch (IllegalArgumentException e) {
                doctorUsers = userRepository.findByRole(Role.ROLE_CLINICIAN);
            }
        } else {
            doctorUsers = userRepository.findByRole(Role.ROLE_CLINICIAN);
        }

        return doctorUsers.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> {
                    ClinicianProfile profile = clinicianProfileRepository != null
                            ? clinicianProfileRepository.findByUserId(user.getId()).orElse(null)
                            : null;
                    return DoctorRegistrationResponseDTO.fromUserAndProfile(user, profile);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public DoctorRegistrationResponseDTO approveDoctorRequest(UUID userId, UserPrincipal adminPrincipal, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with ID: " + userId));

        if (user.getRole() != Role.ROLE_CLINICIAN) {
            throw new IllegalArgumentException("Target user is not a clinician/doctor: " + userId);
        }

        AccountStatus previousStatus = user.getStatus();
        user.setStatus(AccountStatus.ACTIVE);
        user.setActive(true);
        User saved = userRepository.save(user);

        log.info("[ADMIN_DOCTOR_APPROVAL] Admin '{}' approved doctor registration for '{}' (ID: {})",
                adminPrincipal.getUsername(), user.getEmail(), userId);

        User adminUser = userRepository.findById(adminPrincipal.getId()).orElse(null);
        if (auditLogService != null) {
            auditLogService.recordEvent(
                    adminUser,
                    adminPrincipal.getUsername(),
                    null,
                    "DOCTOR_REQUEST_APPROVED",
                    "User",
                    userId,
                    "SUCCESS",
                    clientIp,
                    null,
                    Map.of(
                            "targetEmail", user.getEmail(),
                            "previousStatus", previousStatus != null ? previousStatus.name() : "UNKNOWN",
                            "newStatus", AccountStatus.ACTIVE.name()
                    )
            );
        }

        ClinicianProfile profile = clinicianProfileRepository != null
                ? clinicianProfileRepository.findByUserId(saved.getId()).orElse(null)
                : null;
        return DoctorRegistrationResponseDTO.fromUserAndProfile(saved, profile);
    }

    @Transactional
    public DoctorRegistrationResponseDTO rejectDoctorRequest(UUID userId, UserPrincipal adminPrincipal, String clientIp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor user not found with ID: " + userId));

        if (user.getRole() != Role.ROLE_CLINICIAN) {
            throw new IllegalArgumentException("Target user is not a clinician/doctor: " + userId);
        }

        AccountStatus previousStatus = user.getStatus();
        user.setStatus(AccountStatus.REJECTED);
        user.setActive(true);
        User saved = userRepository.save(user);

        log.info("[ADMIN_DOCTOR_REJECTION] Admin '{}' rejected doctor registration for '{}' (ID: {})",
                adminPrincipal.getUsername(), user.getEmail(), userId);

        User adminUser = userRepository.findById(adminPrincipal.getId()).orElse(null);
        if (auditLogService != null) {
            auditLogService.recordEvent(
                    adminUser,
                    adminPrincipal.getUsername(),
                    null,
                    "DOCTOR_REQUEST_REJECTED",
                    "User",
                    userId,
                    "SUCCESS",
                    clientIp,
                    null,
                    Map.of(
                            "targetEmail", user.getEmail(),
                            "previousStatus", previousStatus != null ? previousStatus.name() : "UNKNOWN",
                            "newStatus", AccountStatus.REJECTED.name()
                    )
            );
        }

        ClinicianProfile profile = clinicianProfileRepository != null
                ? clinicianProfileRepository.findByUserId(saved.getId()).orElse(null)
                : null;
        return DoctorRegistrationResponseDTO.fromUserAndProfile(saved, profile);
    }
}
