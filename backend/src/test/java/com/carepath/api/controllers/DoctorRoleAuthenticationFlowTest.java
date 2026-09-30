package com.carepath.api.controllers;

import com.carepath.api.dto.DoctorRegisterRequest;
import com.carepath.api.dto.LoginRequest;
import com.carepath.api.dto.RegisterRequest;
import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class DoctorRoleAuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicianProfileRepository clinicianProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        clinicianProfileRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = new User(
                "admin.test@carepath.io",
                passwordEncoder.encode("AdminPass123!"),
                Role.ROLE_ADMIN,
                "System",
                "Administrator"
        );
        adminUser.setStatus(AccountStatus.ACTIVE);
        adminUser.setActive(true);
        userRepository.save(adminUser);

        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
    }

    @Test
    @DisplayName("Patient Registration creates an immediate ACTIVE account")
    void testPatientRegistrationIsActive() throws Exception {
        RegisterRequest patientReq = new RegisterRequest(
                "patient.test@carepath.io",
                "PatientPass123!",
                "Sarah",
                "Connor"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("patient.test@carepath.io"))
                .andExpect(jsonPath("$.role").value("ROLE_PATIENT"));

        User saved = userRepository.findByEmail("patient.test@carepath.io").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.isActive()).isTrue();

        // Patient can login immediately
        LoginRequest loginReq = new LoginRequest("patient.test@carepath.io", "PatientPass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ROLE_PATIENT"));
    }

    @Test
    @DisplayName("Doctor Registration creates account with status PENDING and creates ClinicianProfile")
    void testDoctorRegistrationCreatesPendingAccount() throws Exception {
        DoctorRegisterRequest docReq = new DoctorRegisterRequest(
                "Marcus",
                "Vance",
                "doc.marcus@carepath.io",
                "DoctorPass123!",
                "MD-12345678",
                "Cardiology",
                "CarePath Heart Center",
                "+15554443322"
        );

        mockMvc.perform(post("/api/v1/auth/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("doc.marcus@carepath.io"))
                .andExpect(jsonPath("$.role").value("ROLE_CLINICIAN"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value(containsString("admin approval")));

        User savedUser = userRepository.findByEmail("doc.marcus@carepath.io").orElseThrow();
        assertThat(savedUser.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(savedUser.getRole()).isEqualTo(Role.ROLE_CLINICIAN);

        ClinicianProfile savedProfile = clinicianProfileRepository.findByUserId(savedUser.getId()).orElseThrow();
        assertThat(savedProfile.getLicenseNumber()).isEqualTo("MD-12345678");
        assertThat(savedProfile.getSpecialty()).isEqualTo("Cardiology");
        assertThat(savedProfile.getClinicName()).isEqualTo("CarePath Heart Center");
    }

    @Test
    @DisplayName("Doctor with status PENDING cannot login and receives clear pending message")
    void testPendingDoctorCannotLogin() throws Exception {
        DoctorRegisterRequest docReq = new DoctorRegisterRequest(
                "Gregory",
                "House",
                "doc.house@carepath.io",
                "HouseMD123!",
                "MD-99887766",
                "Diagnostic Medicine",
                "Princeton-Plainsboro",
                "+15557778899"
        );

        mockMvc.perform(post("/api/v1/auth/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated());

        // Attempt login while PENDING
        LoginRequest loginReq = new LoginRequest("doc.house@carepath.io", "HouseMD123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("DOCTOR_PENDING_APPROVAL"))
                .andExpect(jsonPath("$.message").value(containsString("pending admin approval")));
    }

    @Test
    @DisplayName("Admin can view, approve, and activate a pending doctor registration request")
    void testAdminApprovalWorkflow() throws Exception {
        DoctorRegisterRequest docReq = new DoctorRegisterRequest(
                "Allison",
                "Cameron",
                "doc.cameron@carepath.io",
                "CameronPass123!",
                "MD-44332211",
                "Immunology",
                "Princeton Hospital",
                "+15551122334"
        );

        mockMvc.perform(post("/api/v1/auth/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated());

        User docUser = userRepository.findByEmail("doc.cameron@carepath.io").orElseThrow();

        // 1. Admin views doctor requests
        mockMvc.perform(get("/api/v1/admin/doctor-requests")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].email").value("doc.cameron@carepath.io"))
                .andExpect(jsonPath("$[0].licenseNumber").value("MD-44332211"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // 2. Admin approves doctor request
        mockMvc.perform(post("/api/v1/admin/doctor-requests/" + docUser.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(docUser.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        User approvedUser = userRepository.findById(docUser.getId()).orElseThrow();
        assertThat(approvedUser.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(approvedUser.isActive()).isTrue();

        // 3. Approved Doctor can now login successfully
        LoginRequest loginReq = new LoginRequest("doc.cameron@carepath.io", "CameronPass123!");
        String docTokenJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ROLE_CLINICIAN"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        String docToken = objectMapper.readTree(docTokenJson).get("accessToken").asText();

        // 4. Approved doctor can access Doctor Dashboard endpoint
        mockMvc.perform(get("/api/v1/doctor/dashboard")
                        .header("Authorization", "Bearer " + docToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.licenseNumber").value("MD-44332211"));
    }

    @Test
    @DisplayName("Admin can reject doctor request and rejected doctor cannot access dashboard")
    void testAdminRejectionWorkflow() throws Exception {
        DoctorRegisterRequest docReq = new DoctorRegisterRequest(
                "Robert",
                "Chase",
                "doc.chase@carepath.io",
                "ChasePass123!",
                "MD-55667788",
                "Intensive Care",
                "CarePath Medical",
                "+15559988776"
        );

        mockMvc.perform(post("/api/v1/auth/doctor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReq)))
                .andExpect(status().isCreated());

        User docUser = userRepository.findByEmail("doc.chase@carepath.io").orElseThrow();

        // Admin rejects doctor request
        mockMvc.perform(post("/api/v1/admin/doctor-requests/" + docUser.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(docUser.getId().toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        User rejectedUser = userRepository.findById(docUser.getId()).orElseThrow();
        assertThat(rejectedUser.getStatus()).isEqualTo(AccountStatus.REJECTED);

        // Rejected doctor login attempt receives rejected message
        LoginRequest loginReq = new LoginRequest("doc.chase@carepath.io", "ChasePass123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("DOCTOR_REGISTRATION_REJECTED"))
                .andExpect(jsonPath("$.message").value(containsString("not approved")));
    }

    @Test
    @DisplayName("Role-Based Authorization Rules: Patient cannot access Admin or Doctor endpoints")
    void testRoleAccessRestrictions() throws Exception {
        User patient = new User(
                "patient.isolation@carepath.io",
                passwordEncoder.encode("Pass123!"),
                Role.ROLE_PATIENT,
                "Pat",
                "Test"
        );
        patient.setStatus(AccountStatus.ACTIVE);
        userRepository.save(patient);
        String patientToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(patient));

        // Patient attempting to access Admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/doctor-requests")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        // Patient attempting to access Doctor endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/v1/doctor/dashboard")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }
}
