package com.carepath.api.controllers;

import com.carepath.api.dto.PatientIntakeRequestDTO;
import com.carepath.api.dto.PatientProfileRequestDTO;
import com.carepath.domain.enums.AlcoholUse;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.Role;
import com.carepath.domain.enums.SmokingStatus;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.PatientRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

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
                "Marcus",
                "Admin"
        );
        adminUser = userRepository.save(adminUser);
        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));

        profileA = new PatientProfile(
                userA,
                LocalDate.of(1985, 3, 15),
                BiologicalSex.FEMALE,
                new BigDecimal("165.00"),
                new BigDecimal("68.00")
        );
        profileA.setSmokingStatus(SmokingStatus.NEVER);
        profileA.setAlcoholUse(AlcoholUse.NONE);
        profileA.setMedicalHistory("{\"hypertensionHistory\": false}");
        profileA = patientRepository.save(profileA);

        profileB = new PatientProfile(
                userB,
                LocalDate.of(1980, 7, 22),
                BiologicalSex.MALE,
                new BigDecimal("180.00"),
                new BigDecimal("85.00")
        );
        profileB.setSmokingStatus(SmokingStatus.FORMER);
        profileB.setAlcoholUse(AlcoholUse.OCCASIONAL);
        profileB.setMedicalHistory("{\"diabetesHistory\": true}");
        profileB = patientRepository.save(profileB);
    }

    @Test
    @DisplayName("Should reject unauthenticated requests to patient profile with 401 Unauthorized")
    void testGetProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/patients/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should retrieve own patient profile via /api/v1/patients/me")
    void testGetOwnProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(profileA.getId().toString()))
                .andExpect(jsonPath("$.userId").value(userA.getId().toString()))
                .andExpect(jsonPath("$.email").value(userA.getEmail()))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Anderson"))
                .andExpect(jsonPath("$.biologicalSex").value("FEMALE"))
                .andExpect(jsonPath("$.heightCm").value(165.00))
                .andExpect(jsonPath("$.baselineWeightKg").value(68.00))
                .andExpect(jsonPath("$.currentBmi").value(24.98)) // 68 / (1.65)^2 = 24.977... -> 24.98
                .andExpect(jsonPath("$.smokingStatus").value("NEVER"))
                .andExpect(jsonPath("$.alcoholUse").value("NONE"))
                .andExpect(jsonPath("$.medicalHistory.hypertensionHistory").value(false));
    }

    @Test
    @DisplayName("Should return 404 when authenticated patient has not created a profile yet")
    void testGetProfileWhenNotCreated() throws Exception {
        User userWithoutProfile = new User(
                "no.profile." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("SecurePassword123!"),
                Role.ROLE_PATIENT,
                "New",
                "Patient"
        );
        userWithoutProfile = userRepository.save(userWithoutProfile);
        String tokenWithoutProfile = jwtTokenProvider.generateAccessToken(UserPrincipal.create(userWithoutProfile));

        mockMvc.perform(get("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + tokenWithoutProfile))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should update own profile and recompute BMI via /api/v1/patients/me")
    void testUpdateOwnProfile() throws Exception {
        PatientProfileRequestDTO updateRequest = new PatientProfileRequestDTO(
                LocalDate.of(1985, 3, 15),
                BiologicalSex.FEMALE,
                new BigDecimal("165.00"),
                new BigDecimal("62.50"), // Weight reduced
                SmokingStatus.NEVER,
                AlcoholUse.OCCASIONAL,
                Map.of("hypertensionHistory", false, "exerciseWeekly", true)
        );
        updateRequest.setPhone("+15559876543");

        mockMvc.perform(put("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(profileA.getId().toString()))
                .andExpect(jsonPath("$.baselineWeightKg").value(62.50))
                .andExpect(jsonPath("$.currentBmi").value(22.96)) // 62.5 / (1.65)^2 = 22.956... -> 22.96
                .andExpect(jsonPath("$.alcoholUse").value("OCCASIONAL"))
                .andExpect(jsonPath("$.medicalHistory.exerciseWeekly").value(true))
                .andExpect(jsonPath("$.phone").value("+15559876543"));

        // Verify audit log entry was created
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs).anyMatch(log -> profileA.getId().equals(log.getTargetPatientId()) && "PROFILE_UPSERT".equals(log.getActionType()));
    }

    @Test
    @DisplayName("STRICT ISOLATION: User A cannot access Patient B profile via ID (403 Forbidden)")
    void testStrictOwnershipIsolationOnRead() throws Exception {
        // User A accessing Patient B -> 403 Forbidden
        mockMvc.perform(get("/api/v1/patients/" + profileB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));

        // User B accessing Patient A -> 403 Forbidden
        mockMvc.perform(get("/api/v1/patients/" + profileA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        // User A accessing Patient A -> 200 OK
        mockMvc.perform(get("/api/v1/patients/" + profileA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(profileA.getId().toString()));
    }

    @Test
    @DisplayName("STRICT ISOLATION: User A cannot update Patient B profile via ID (403 Forbidden)")
    void testStrictOwnershipIsolationOnUpdate() throws Exception {
        PatientProfileRequestDTO maliciousUpdate = new PatientProfileRequestDTO(
                LocalDate.of(1980, 7, 22),
                BiologicalSex.MALE,
                new BigDecimal("180.00"),
                new BigDecimal("99.00"),
                SmokingStatus.CURRENT_DAILY,
                AlcoholUse.HEAVY,
                Map.of("hacked", true)
        );

        mockMvc.perform(put("/api/v1/patients/" + profileB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        // Verify Patient B remained unchanged
        PatientProfile unhackedB = patientRepository.findById(profileB.getId()).orElseThrow();
        assertThat(unhackedB.getBaselineWeightKg()).isEqualByComparingTo("85.00");
    }

    @Test
    @DisplayName("Admin role CAN view patient profile by ID")
    void testAdminCanViewPatientProfile() throws Exception {
        mockMvc.perform(get("/api/v1/patients/" + profileA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(profileA.getId().toString()))
                .andExpect(jsonPath("$.email").value(userA.getEmail()));
    }

    @Test
    @DisplayName("Should reject invalid physiological values with 400 Bad Request")
    void testValidationRejection() throws Exception {
        // Height below min (50.0 cm)
        PatientProfileRequestDTO invalidHeight = new PatientProfileRequestDTO(
                LocalDate.of(1990, 1, 1),
                BiologicalSex.FEMALE,
                new BigDecimal("30.00"),
                new BigDecimal("60.00"),
                SmokingStatus.NEVER,
                AlcoholUse.NONE,
                Map.of()
        );

        mockMvc.perform(put("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHeight)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(1))));

        // Future date of birth
        PatientProfileRequestDTO futureDob = new PatientProfileRequestDTO(
                LocalDate.now().plusDays(1),
                BiologicalSex.FEMALE,
                new BigDecimal("165.00"),
                new BigDecimal("60.00"),
                SmokingStatus.NEVER,
                AlcoholUse.NONE,
                Map.of()
        );

        mockMvc.perform(put("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureDob)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Should submit and retrieve intake successfully")
    void testIntakeFlow() throws Exception {
        User newPatient = new User(
                "intake.test." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode("SecurePassword123!"),
                Role.ROLE_PATIENT,
                "Intake",
                "Tester"
        );
        newPatient = userRepository.save(newPatient);
        String intakeToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(newPatient));

        PatientIntakeRequestDTO intakeRequest = new PatientIntakeRequestDTO(
                LocalDate.of(1992, 11, 5),
                BiologicalSex.MALE,
                new BigDecimal("178.00"),
                new BigDecimal("75.00"),
                SmokingStatus.CURRENT_OCCASIONAL,
                AlcoholUse.OCCASIONAL,
                Map.of("asthmaHistory", true)
        );

        // POST /api/v1/patients/intake -> 201 Created
        mockMvc.perform(post("/api/v1/patients/intake")
                        .header("Authorization", "Bearer " + intakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intakeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").isNotEmpty())
                .andExpect(jsonPath("$.currentBmi").value(23.67)) // 75 / (1.78)^2 = 23.671... -> 23.67
                .andExpect(jsonPath("$.message").value(containsString("successfully")));

        // GET /api/v1/patients/intake -> 200 OK
        mockMvc.perform(get("/api/v1/patients/intake")
                        .header("Authorization", "Bearer " + intakeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biologicalSex").value("MALE"))
                .andExpect(jsonPath("$.heightCm").value(178.00))
                .andExpect(jsonPath("$.baselineWeightKg").value(75.00))
                .andExpect(jsonPath("$.currentBmi").value(23.67))
                .andExpect(jsonPath("$.medicalHistory.asthmaHistory").value(true));

        // PUT /api/v1/patients/intake -> 200 OK
        intakeRequest.setBaselineWeightKg(new BigDecimal("73.50"));
        mockMvc.perform(put("/api/v1/patients/intake")
                        .header("Authorization", "Bearer " + intakeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intakeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baselineWeightKg").value(73.50))
                .andExpect(jsonPath("$.currentBmi").value(23.20));
    }

    @Test
    @DisplayName("Patient cannot change role through profile API")
    void testPatientCannotElevateRoleViaProfileUpdate() throws Exception {
        PatientProfileRequestDTO updateRequest = new PatientProfileRequestDTO(
                LocalDate.of(1985, 3, 15),
                BiologicalSex.FEMALE,
                new BigDecimal("165.00"),
                new BigDecimal("68.00"),
                SmokingStatus.NEVER,
                AlcoholUse.NONE,
                Map.of("role", "ROLE_ADMIN")
        );

        mockMvc.perform(put("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Verify User A role is still ROLE_PATIENT in DB
        User reloadedUserA = userRepository.findById(userA.getId()).orElseThrow();
        assertThat(reloadedUserA.getRole()).isEqualTo(Role.ROLE_PATIENT);
    }
}
