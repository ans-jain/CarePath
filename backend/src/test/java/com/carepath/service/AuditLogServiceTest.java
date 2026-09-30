package com.carepath.service;

import com.carepath.domain.enums.Role;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private AuditLogService auditLogService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditLogService = new AuditLogService(auditLogRepository, userRepository, objectMapper);

        sampleUserId = UUID.randomUUID();
        sampleUser = new User("sarah@carepath.io", "hashedPass", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        sampleUser.setId(sampleUserId);
    }

    @Test
    @DisplayName("Should successfully record audit event and sanitize sensitive tokens/passwords")
    void testRecordEventSanitizesSecrets() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> details = Map.of(
                "username", "sarah@carepath.io",
                "password", "PlaintextPassword123!",
                "token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                "action", "LOGIN"
        );

        AuditLog saved = auditLogService.recordEvent(
                sampleUser,
                sampleUser.getEmail(),
                null,
                "LOGIN_SUCCESS",
                "User",
                sampleUserId,
                "SUCCESS",
                "192.168.1.100",
                "Mozilla/5.0",
                details
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getActionType()).isEqualTo("LOGIN_SUCCESS");
        assertThat(saved.getActorEmail()).isEqualTo("sarah@carepath.io");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");

        // Verify sensitive fields are redacted from details JSON
        assertThat(saved.getDetails()).contains("\"action\":\"LOGIN\"");
        assertThat(saved.getDetails()).contains("\"password\":\"[REDACTED]\"");
        assertThat(saved.getDetails()).contains("\"token\":\"[REDACTED]\"");
        assertThat(saved.getDetails()).doesNotContain("PlaintextPassword123!");
    }

    @Test
    @DisplayName("Should support unauthenticated failed login audit event with null actor user")
    void testRecordUnauthenticatedEvent() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        AuditLog saved = auditLogService.recordEvent(
                null,
                "unknown.attacker@carepath.io",
                null,
                "LOGIN_FAILURE",
                "User",
                null,
                "FAILURE",
                "203.0.113.42",
                "curl/7.68.0",
                Map.of("reason", "Bad credentials")
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getActorUser()).isNull();
        assertThat(saved.getActorEmail()).isEqualTo("unknown.attacker@carepath.io");
        assertThat(saved.getActionType()).isEqualTo("LOGIN_FAILURE");
        assertThat(saved.getStatus()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("Should handle database persistence error gracefully without rethrowing")
    void testRecordEventHandlesFailureGracefully() {
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB Connection Timeout"));

        AuditLog result = auditLogService.recordEvent(
                sampleUser,
                sampleUser.getEmail(),
                null,
                "PROFILE_UPDATE",
                "PatientProfile",
                UUID.randomUUID(),
                "SUCCESS",
                "127.0.0.1",
                null,
                Map.of("field", "weight")
        );

        // Fail-safe: returns null, does not crash surrounding application flow
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should query audit logs with dynamic specification")
    void testQueryAuditLogs() {
        AuditLog entry = new AuditLog(sampleUser, null, "LOGIN_SUCCESS", "User", sampleUserId, "127.0.0.1", "{}");
        Page<AuditLog> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);

        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLog> result = auditLogService.queryAuditLogs(
                sampleUserId,
                "LOGIN_SUCCESS",
                "User",
                "SUCCESS",
                Instant.now().minusSeconds(3600),
                Instant.now(),
                PageRequest.of(0, 20)
        );

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).getActionType()).isEqualTo("LOGIN_SUCCESS");
    }
}
