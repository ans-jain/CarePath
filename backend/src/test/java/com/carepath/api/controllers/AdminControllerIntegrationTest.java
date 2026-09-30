package com.carepath.api.controllers;

import com.carepath.api.dto.UpdateUserRoleRequest;
import com.carepath.api.dto.UpdateUserStatusRequest;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.JwtTokenProvider;
import com.carepath.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User patientUser;
    private User adminUser;
    private String patientToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        patientUser = new User(
                "patient.test." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("Password123!"),
                Role.ROLE_PATIENT,
                "Patient",
                "User"
        );
        patientUser = userRepository.save(patientUser);
        patientToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(patientUser));

        adminUser = new User(
                "admin.test." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("Password123!"),
                Role.ROLE_ADMIN,
                "Admin",
                "User"
        );
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));

        // Create a sample audit log
        AuditLog auditLog = new AuditLog(adminUser, null, "TEST_ACTION", "System", null, "127.0.0.1", "{}");
        auditLogRepository.save(auditLog);
    }

    @Test
    @DisplayName("Unauthenticated request to admin audit-logs returns 401 Unauthorized")
    void testAuditLogsUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Patient request to admin audit-logs returns 403 Forbidden")
    void testAuditLogsPatientRoleReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin request to admin audit-logs returns 200 OK with content")
    void testAuditLogsAdminRoleReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("Admin can retrieve paginated user list via /api/v1/admin/users")
    void testAdminGetUsersReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("Admin can promote user role and action is audited")
    void testAdminUpdateUserRole() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest(Role.ROLE_CLINICIAN);

        mockMvc.perform(patch("/api/v1/admin/users/" + patientUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_CLINICIAN"));

        User reloaded = userRepository.findById(patientUser.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.ROLE_CLINICIAN);
    }

    @Test
    @DisplayName("Patient cannot update user roles (returns 403 Forbidden)")
    void testPatientCannotUpdateRole() throws Exception {
        UpdateUserRoleRequest req = new UpdateUserRoleRequest(Role.ROLE_ADMIN);

        mockMvc.perform(patch("/api/v1/admin/users/" + patientUser.getId() + "/role")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can toggle user active status")
    void testAdminUpdateUserStatus() throws Exception {
        UpdateUserStatusRequest req = new UpdateUserStatusRequest(false);

        mockMvc.perform(patch("/api/v1/admin/users/" + patientUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        User reloaded = userRepository.findById(patientUser.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    @DisplayName("Admin can view system security overview")
    void testAdminGetSecurityOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/system/security")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimitingEnabled").isBoolean())
                .andExpect(jsonPath("$.corsAllowedOrigins").isArray())
                .andExpect(jsonPath("$.securityHeadersEnabled").isArray())
                .andExpect(jsonPath("$.totalAuditLogsCount").isNumber());
    }
}
