package com.carepath.service;

import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private static final Set<String> REDACTED_KEYS = Set.of(
            "password", "passwordhash", "secret", "token", "accesstoken",
            "refreshtoken", "authorization", "apikey", "credential"
    );

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records an immutable audit log entry.
     */
    @Transactional
    public AuditLog recordEvent(User actorUser,
                                String actorEmail,
                                UUID targetPatientId,
                                String actionType,
                                String entityName,
                                UUID entityId,
                                String status,
                                String ipAddress,
                                String userAgent,
                                Map<String, Object> details) {
        try {
            String sanitizedJson = sanitizeAndSerialize(details);

            String resolvedEmail = actorEmail;
            if (resolvedEmail == null && actorUser != null) {
                resolvedEmail = actorUser.getEmail();
            }

            User attachedUser = actorUser;
            if (attachedUser != null && attachedUser.getId() != null) {
                if (!userRepository.existsById(attachedUser.getId())) {
                    attachedUser = null;
                }
            }

            AuditLog entry = new AuditLog(
                    attachedUser,
                    resolvedEmail,
                    targetPatientId,
                    actionType,
                    entityName,
                    entityId,
                    ipAddress != null ? ipAddress : "127.0.0.1",
                    userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent,
                    status != null ? status : "SUCCESS",
                    sanitizedJson
            );

            AuditLog saved = auditLogRepository.save(entry);
            log.info("[AUDIT_RECORDED] action='{}', entity='{}', id={}, status='{}', actor='{}', ip='{}'",
                    actionType, entityName, entityId, status, resolvedEmail, ipAddress);
            return saved;
        } catch (Exception ex) {
            log.error("[AUDIT_FAILURE] Failed to persist audit log entry: {}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Helper overload for user-based actions where actor is known.
     */
    @Transactional
    public AuditLog recordUserEvent(User actorUser,
                                    UUID targetPatientId,
                                    String actionType,
                                    String entityName,
                                    UUID entityId,
                                    String status,
                                    String ipAddress,
                                    String userAgent,
                                    Map<String, Object> details) {
        return recordEvent(actorUser, actorUser != null ? actorUser.getEmail() : null,
                targetPatientId, actionType, entityName, entityId, status, ipAddress, userAgent, details);
    }

    /**
     * Queries audit logs with dynamic filtering for administrative auditing.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> queryAuditLogs(UUID actorUserId,
                                         String actionType,
                                         String entityName,
                                         String status,
                                         Instant startDate,
                                         Instant endDate,
                                         Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorUserId != null) {
                predicates.add(cb.equal(root.get("actorUser").get("id"), actorUserId));
            }
            if (actionType != null && !actionType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("actionType")), actionType.trim().toUpperCase()));
            }
            if (entityName != null && !entityName.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("entityName")), entityName.trim().toUpperCase()));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable);
    }

    /**
     * Sanitizes map by stripping or redacting sensitive keys and clinical PHI before JSON serialization.
     */
    private String sanitizeAndSerialize(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "{}";
        }
        try {
            Map<String, Object> sanitized = new HashMap<>();
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                String key = entry.getKey();
                String lowerKey = key.toLowerCase().replaceAll("[_-]", "");
                if (REDACTED_KEYS.contains(lowerKey)) {
                    sanitized.put(key, "[REDACTED]");
                } else {
                    sanitized.put(key, entry.getValue());
                }
            }
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            log.warn("Failed to serialize audit log details: {}", e.getMessage());
            return "{}";
        }
    }
}
