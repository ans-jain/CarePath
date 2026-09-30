package com.carepath.api.controllers;

import com.carepath.api.dto.*;
import com.carepath.domain.enums.*;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.JwtTokenProvider;
import com.carepath.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FullSystemE2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        adminUser = new User(
                "e2e-admin-" + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("AdminPass123!"),
                Role.ROLE_ADMIN,
                "Admin",
                "Director"
        );
        userRepository.save(adminUser);
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
    }

    @Test
    @DisplayName("Complete E2E lifecycle: Auth -> Intake Profile -> Vitals Ingestion -> Risk Assessment -> Notifications -> Audit Trail")
    void testCompleteSystemFlow() throws Exception {
        String correlationId = "e2e-flow-" + UUID.randomUUID();
        String patientEmail = "e2e-patient-" + UUID.randomUUID() + "@carepath.io";

        // 1. User Registration (Public endpoint)
        RegisterRequest regReq = new RegisterRequest();
        regReq.setEmail(patientEmail);
        regReq.setPassword("SecurePass123!");
        regReq.setFirstName("Elena");
        regReq.setLastName("Rostova");
        regReq.setRole(Role.ROLE_PATIENT);

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Request-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-ID", correlationId))
                .andExpect(jsonPath("$.email").value(patientEmail))
                .andExpect(jsonPath("$.role").value("ROLE_PATIENT"));

        // Login to acquire accessToken
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(patientEmail);
        loginReq.setPassword("SecurePass123!");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Request-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String patientToken = loginJson.path("accessToken").asText();

        // 2. Health Intake / Profile Upsert
        PatientProfileRequestDTO intakeReq = new PatientProfileRequestDTO();
        intakeReq.setDateOfBirth(LocalDate.of(1982, 3, 14));
        intakeReq.setBiologicalSex(BiologicalSex.FEMALE);
        intakeReq.setHeightCm(new BigDecimal("168.00"));
        intakeReq.setBaselineWeightKg(new BigDecimal("72.50"));

        mockMvc.perform(put("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + patientToken)
                        .header("X-Request-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intakeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biologicalSex").value("FEMALE"));

        // 3. Vitals Ingestion
        CreateVitalRequestDTO vitalReq = new CreateVitalRequestDTO();
        vitalReq.setMetricType(MetricType.SYSTOLIC_BP);
        vitalReq.setValue(new BigDecimal("132.00"));
        vitalReq.setUnit("mmHg");
        vitalReq.setContext(MeasurementContext.RESTING);
        vitalReq.setSource(MeasurementSource.MANUAL);

        mockMvc.perform(post("/api/v1/vitals")
                        .header("Authorization", "Bearer " + patientToken)
                        .header("X-Request-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vitalReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricType").value("SYSTOLIC_BP"))
                .andExpect(jsonPath("$.value").value(132.00));

        // 4. Risk Assessment Recording
        RiskAssessmentRecordDTO assessReq = new RiskAssessmentRecordDTO();
        assessReq.setModelVersion("calibrated_v1.0.0");
        assessReq.setOverallRiskScore(new BigDecimal("0.380"));
        assessReq.setRiskCategory(RiskCategory.MODERATE);
        assessReq.setConfidenceLevel(ConfidenceLevel.LONGITUDINAL_ROBUST);
        assessReq.setFeatureSnapshot("{\"systolic_bp\": 132.0, \"bmi\": 25.7}");

        mockMvc.perform(post("/api/v1/risk-assessments")
                        .header("Authorization", "Bearer " + patientToken)
                        .header("X-Request-ID", correlationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assessReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskCategory").value("MODERATE"))
                .andExpect(jsonPath("$.overallRiskScore").value(0.380));

        // 5. Query Latest Assessment
        mockMvc.perform(get("/api/v1/risk-assessments/latest")
                        .header("Authorization", "Bearer " + patientToken)
                        .header("X-Request-ID", correlationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskCategory").value("MODERATE"));

        // 6. Notifications Retrieval
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + patientToken)
                        .header("X-Request-ID", correlationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // 7. Admin Security Overview & Audit Logs
        mockMvc.perform(get("/api/v1/admin/security/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Request-ID", correlationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateLimitingEnabled").value(true));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Request-ID", correlationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
