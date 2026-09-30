package com.carepath.api.controllers;

import com.carepath.api.dto.BatchCreateVitalRequestDTO;
import com.carepath.api.dto.BatchVitalEntryDTO;
import com.carepath.api.dto.CreateVitalRequestDTO;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.carepath.domain.enums.MetricType;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.models.VitalMetric;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.domain.repository.VitalRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VitalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VitalRepository vitalRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User userA;
    private User userB;
    private User adminUser;
    private String tokenA;
    private String tokenB;
    private String adminToken;

    private PatientProfile profileA;
    private PatientProfile profileB;

    @BeforeEach
    void setUp() {
        userA = new User(
                "patient.a." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("SecurePassword123!"),
                Role.ROLE_PATIENT,
                "Alice",
                "Anderson"
        );
        userA = userRepository.save(userA);
        tokenA = jwtTokenProvider.generateAccessToken(UserPrincipal.create(userA));

        userB = new User(
                "patient.b." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("SecurePassword123!"),
                Role.ROLE_PATIENT,
                "Bob",
                "Baker"
        );
        userB = userRepository.save(userB);
        tokenB = jwtTokenProvider.generateAccessToken(UserPrincipal.create(userB));

        adminUser = new User(
                "admin." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("SecurePassword123!"),
                Role.ROLE_ADMIN,
                "Admin",
                "Director"
        );
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));

        profileA = new PatientProfile(
                userA,
                LocalDate.of(1985, 3, 15),
                BiologicalSex.FEMALE,
                new BigDecimal("168.00"),
                new BigDecimal("65.00")
        );
        profileA = patientRepository.save(profileA);

        profileB = new PatientProfile(
                userB,
                LocalDate.of(1992, 7, 24),
                BiologicalSex.MALE,
                new BigDecimal("182.00"),
                new BigDecimal("84.00")
        );
        profileB = patientRepository.save(profileB);
    }

    @Test
    @DisplayName("POST /api/v1/vitals - Successfully record single vital")
    void testRecordSingleVitalSuccess() throws Exception {
        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.SYSTOLIC_BP);
        request.setValue(new BigDecimal("128.50"));
        request.setMeasurementContext(MeasurementContext.RESTING);
        request.setSource(MeasurementSource.MANUAL);

        mockMvc.perform(post("/api/v1/vitals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.patientId").value(profileA.getId().toString()))
                .andExpect(jsonPath("$.metricType").value("SYSTOLIC_BP"))
                .andExpect(jsonPath("$.value").value(128.50))
                .andExpect(jsonPath("$.unit").value("mmHg"))
                .andExpect(jsonPath("$.measurementContext").value("RESTING"))
                .andExpect(jsonPath("$.source").value("MANUAL"));

        assertThat(vitalRepository.countByPatientId(profileA.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/patients/{patientId}/vitals - Successfully record vital by patient ID route")
    void testRecordSingleVitalByPatientRoute() throws Exception {
        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("72.00"));

        mockMvc.perform(post("/api/v1/patients/" + profileA.getId() + "/vitals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricType").value("HEART_RATE"))
                .andExpect(jsonPath("$.unit").value("bpm"));
    }

    @Test
    @DisplayName("POST /api/v1/vitals - Reject physiologically impossible value (400 Bad Request)")
    void testRecordSingleVitalPhysiologicalViolation() throws Exception {
        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.SYSTOLIC_BP);
        request.setValue(new BigDecimal("350.00")); // Impossible systolic BP (>300)

        mockMvc.perform(post("/api/v1/vitals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail", containsString("Physiological range violation: Systolic blood pressure")));

        assertThat(vitalRepository.countByPatientId(profileA.getId())).isZero();
    }

    @Test
    @DisplayName("POST /api/v1/vitals - Reject future timestamp (400 Bad Request)")
    void testRecordSingleVitalFutureTimestamp() throws Exception {
        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("72.00"));
        request.setRecordedAt(Instant.now().plus(2, ChronoUnit.DAYS));

        mockMvc.perform(post("/api/v1/vitals")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("future")));

        assertThat(vitalRepository.countByPatientId(profileA.getId())).isZero();
    }

    @Test
    @DisplayName("POST /api/v1/vitals/batch - Successfully record batch vitals atomically")
    void testRecordBatchVitalsSuccess() throws Exception {
        BatchVitalEntryDTO e1 = new BatchVitalEntryDTO(MetricType.SYSTOLIC_BP, new BigDecimal("122.00"), "mmHg");
        BatchVitalEntryDTO e2 = new BatchVitalEntryDTO(MetricType.DIASTOLIC_BP, new BigDecimal("80.00"), "mmHg");
        BatchVitalEntryDTO e3 = new BatchVitalEntryDTO(MetricType.HEART_RATE, new BigDecimal("65.00"), "bpm");
        BatchVitalEntryDTO e4 = new BatchVitalEntryDTO(MetricType.FASTING_GLUCOSE, new BigDecimal("98.00"), "mg/dL");

        BatchCreateVitalRequestDTO batchRequest = new BatchCreateVitalRequestDTO(
                Instant.now(),
                MeasurementContext.RESTING,
                MeasurementSource.MANUAL,
                List.of(e1, e2, e3, e4)
        );

        mockMvc.perform(post("/api/v1/vitals/batch")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].metricType").value("SYSTOLIC_BP"))
                .andExpect(jsonPath("$[1].metricType").value("DIASTOLIC_BP"))
                .andExpect(jsonPath("$[2].metricType").value("HEART_RATE"))
                .andExpect(jsonPath("$[3].metricType").value("FASTING_GLUCOSE"));

        assertThat(vitalRepository.countByPatientId(profileA.getId())).isEqualTo(4);
    }

    @Test
    @DisplayName("POST /api/v1/vitals/batch - Reject entire batch if one entry has physiological violation")
    void testRecordBatchVitalsViolationRollback() throws Exception {
        BatchVitalEntryDTO e1 = new BatchVitalEntryDTO(MetricType.SYSTOLIC_BP, new BigDecimal("122.00"), "mmHg");
        BatchVitalEntryDTO e2 = new BatchVitalEntryDTO(MetricType.HEART_RATE, new BigDecimal("500.00"), "bpm"); // Invalid HR (>260)

        BatchCreateVitalRequestDTO batchRequest = new BatchCreateVitalRequestDTO(
                Instant.now(),
                List.of(e1, e2)
        );

        mockMvc.perform(post("/api/v1/vitals/batch")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Physiological range violation: Heart rate")));

        assertThat(vitalRepository.countByPatientId(profileA.getId())).isZero();
    }

    @Test
    @DisplayName("GET /api/v1/vitals - Query longitudinal vitals with pagination and filters")
    void testGetVitalsLongitudinal() throws Exception {
        // Seed 3 vitals for patient A
        VitalMetric v1 = new VitalMetric(profileA, Instant.now().minus(3, ChronoUnit.DAYS), MetricType.SYSTOLIC_BP,
                new BigDecimal("120.00"), "mmHg", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        VitalMetric v2 = new VitalMetric(profileA, Instant.now().minus(2, ChronoUnit.DAYS), MetricType.SYSTOLIC_BP,
                new BigDecimal("125.00"), "mmHg", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        VitalMetric v3 = new VitalMetric(profileA, Instant.now().minus(1, ChronoUnit.DAYS), MetricType.HEART_RATE,
                new BigDecimal("70.00"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL);

        vitalRepository.saveAll(List.of(v1, v2, v3));

        // Query all for patient A
        mockMvc.perform(get("/api/v1/vitals")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content", hasSize(3)));

        // Query filtered by metricType
        mockMvc.perform(get("/api/v1/vitals?metricType=SYSTOLIC_BP")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].metricType").value("SYSTOLIC_BP"))
                .andExpect(jsonPath("$.content[1].metricType").value("SYSTOLIC_BP"));
    }

    @Test
    @DisplayName("GET /api/v1/vitals/latest - Retrieve latest vital metric per type")
    void testGetLatestVitals() throws Exception {
        VitalMetric bp1 = new VitalMetric(profileA, Instant.now().minus(2, ChronoUnit.HOURS), MetricType.SYSTOLIC_BP,
                new BigDecimal("120.00"), "mmHg", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        VitalMetric bp2 = new VitalMetric(profileA, Instant.now().minus(1, ChronoUnit.HOURS), MetricType.SYSTOLIC_BP,
                new BigDecimal("130.00"), "mmHg", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        VitalMetric hr1 = new VitalMetric(profileA, Instant.now().minus(3, ChronoUnit.HOURS), MetricType.HEART_RATE,
                new BigDecimal("72.00"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL);

        vitalRepository.saveAll(List.of(bp1, bp2, hr1));

        mockMvc.perform(get("/api/v1/vitals/latest")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.metricType == 'SYSTOLIC_BP')].value").value(hasItem(130.0)))
                .andExpect(jsonPath("$[?(@.metricType == 'HEART_RATE')].value").value(hasItem(72.0)));

        // Query latest with metricType filter
        mockMvc.perform(get("/api/v1/vitals/latest?metricType=SYSTOLIC_BP")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].value").value(130.0));
    }

    @Test
    @DisplayName("GET /api/v1/vitals/{id} - Retrieve vital by ID")
    void testGetVitalById() throws Exception {
        VitalMetric vital = new VitalMetric(profileA, Instant.now(), MetricType.SPO2,
                new BigDecimal("98.00"), "%", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        vital = vitalRepository.save(vital);

        mockMvc.perform(get("/api/v1/vitals/" + vital.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vital.getId().toString()))
                .andExpect(jsonPath("$.metricType").value("SPO2"))
                .andExpect(jsonPath("$.value").value(98.0));
    }

    @Test
    @DisplayName("Cross-patient isolation - Patient B cannot access or record Patient A's vitals (403 Forbidden)")
    void testCrossPatientIsolation() throws Exception {
        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("75.00"));

        // Patient B tries to record vital for Patient A
        mockMvc.perform(post("/api/v1/patients/" + profileA.getId() + "/vitals")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Patient B tries to read Patient A's longitudinal vitals
        mockMvc.perform(get("/api/v1/patients/" + profileA.getId() + "/vitals")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Patient A vital lookup by Patient B
        VitalMetric vitalA = new VitalMetric(profileA, Instant.now(), MetricType.HEART_RATE,
                new BigDecimal("80.00"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        vitalA = vitalRepository.save(vitalA);

        mockMvc.perform(get("/api/v1/vitals/" + vitalA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin role can record and query vitals for any patient")
    void testAdminPrivilege() throws Exception {
        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.FASTING_GLUCOSE);
        request.setValue(new BigDecimal("105.00"));

        mockMvc.perform(post("/api/v1/patients/" + profileA.getId() + "/vitals")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metricType").value("FASTING_GLUCOSE"));

        mockMvc.perform(get("/api/v1/patients/" + profileA.getId() + "/vitals")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @DisplayName("Unauthenticated request receives 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/vitals"))
                .andExpect(status().isUnauthorized());

        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("70.00"));

        mockMvc.perform(post("/api/v1/vitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
