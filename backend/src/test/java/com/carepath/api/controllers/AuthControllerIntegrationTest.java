package com.carepath.api.controllers;

import com.carepath.api.dto.LoginRequest;
import com.carepath.api.dto.RegisterRequest;
import com.carepath.domain.enums.Role;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

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

    private User patientUser;
    private User adminUser;
    private final String rawPassword = "SecurePassword123!";

    @BeforeEach
    void setUp() {
        patientUser = new User(
                "patient." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode(rawPassword),
                Role.ROLE_PATIENT,
                "Sarah",
                "Jenkins"
        );
        patientUser.setPhone("+15551234567");
        patientUser = userRepository.save(patientUser);

        adminUser = new User(
                "admin." + UUID.randomUUID() + "@carepath.io",
                passwordEncoder.encode(rawPassword),
                Role.ROLE_ADMIN,
                "Marcus",
                "Vance"
        );
        adminUser = userRepository.save(adminUser);
    }

    @Test
    @DisplayName("Should successfully register a new user and store BCrypt password hash")
    void testRegisterSuccess() throws Exception {
        String uniqueEmail = "new.patient." + UUID.randomUUID() + "@example.com";
        RegisterRequest request = new RegisterRequest(
                uniqueEmail,
                "ValidP@ssword123!",
                "Alex",
                "Rivera",
                "+15559876543",
                Role.ROLE_PATIENT
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.role").value("ROLE_PATIENT"))
                .andExpect(jsonPath("$.message").value(containsString("successfully")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        // Also test /api/auth/register path mapping
        String responseContent = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        UUID createdUserId = UUID.fromString(jsonNode.get("userId").asText());

        // Verify password hash in PostgreSQL
        User savedUser = userRepository.findById(createdUserId).orElseThrow();
        assertThat(savedUser.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("ValidP@ssword123!", savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("ValidP@ssword123!");
    }

    @Test
    @DisplayName("Should reject duplicate email registration with 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                patientUser.getEmail(),
                "ValidP@ssword123!",
                "Duplicate",
                "User"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    @DisplayName("Should reject invalid email format with 400 Bad Request")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "invalid-email-format",
                "ValidP@ssword123!",
                "Alex",
                "Rivera"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should reject weak password with 400 Bad Request")
    void testRegisterWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user." + UUID.randomUUID() + "@example.com",
                "weak",
                "Alex",
                "Rivera"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Should successfully login and return JWT and refresh cookie")
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest(patientUser.getEmail(), rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.user.id").value(patientUser.getId().toString()))
                .andExpect(jsonPath("$.user.email").value(patientUser.getEmail()))
                .andExpect(jsonPath("$.user.role").value("ROLE_PATIENT"))
                .andExpect(jsonPath("$.user.firstName").value("Sarah"))
                .andExpect(jsonPath("$.user.lastName").value("Jenkins"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("Should reject login with incorrect password with 401 Unauthorized")
    void testLoginIncorrectPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest(patientUser.getEmail(), "WrongPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should reject login with unknown email with 401 Unauthorized")
    void testLoginUnknownEmail() throws Exception {
        LoginRequest loginRequest = new LoginRequest("unknown.user@carepath.io", rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should reject unauthenticated access to /api/auth/me with 401 Unauthorized")
    void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return authenticated user details with valid JWT on /api/auth/me")
    void testGetCurrentUserWithValidJwt() throws Exception {
        UserPrincipal principal = UserPrincipal.create(patientUser);
        String token = jwtTokenProvider.generateAccessToken(principal);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientUser.getId().toString()))
                .andExpect(jsonPath("$.email").value(patientUser.getEmail()))
                .andExpect(jsonPath("$.role").value("ROLE_PATIENT"))
                .andExpect(jsonPath("$.firstName").value("Sarah"))
                .andExpect(jsonPath("$.lastName").value("Jenkins"))
                .andExpect(jsonPath("$.phone").value("+15551234567"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        // Also verify /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientUser.getId().toString()));
    }

    @Test
    @DisplayName("Should reject invalid or malformed JWT on protected endpoint")
    void testProtectedEndpointInvalidJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should reject expired JWT on protected endpoint")
    void testProtectedEndpointExpiredJwt() throws Exception {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                -1000L,
                -1000L
        );
        UserPrincipal principal = UserPrincipal.create(patientUser);
        String expiredToken = expiredProvider.generateAccessToken(principal);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should enforce role-based access control (RBAC)")
    void testRoleBasedAccessControl() throws Exception {
        // Public endpoint allows access without token
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk());

        // Patient token
        String patientToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(patientUser));
        // Admin token
        String adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));

        // Patient can access patient endpoint
        mockMvc.perform(get("/api/test/patient")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("PATIENT")));

        // Patient CANNOT access admin endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));

        // Admin CAN access admin endpoint -> 200 OK
        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("ADMIN")));
    }

    @Test
    @DisplayName("Should refresh access token when valid refresh token is supplied")
    void testRefreshToken() throws Exception {
        UserPrincipal principal = UserPrincipal.create(patientUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(patientUser.getEmail()));
    }

    @Test
    @DisplayName("Should logout and clear refresh token cookie")
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Logged out")))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }
}
