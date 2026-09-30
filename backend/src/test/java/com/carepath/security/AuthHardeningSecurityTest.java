package com.carepath.security;

import com.carepath.api.dto.LoginRequest;
import com.carepath.api.dto.RegisterRequest;
import com.carepath.api.dto.RegisterResponse;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.UserRepository;
import com.carepath.service.AuditLogService;
import com.carepath.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHardeningSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditLogService auditLogService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenProvider,
                auditLogService
        );
    }

    @Test
    @DisplayName("Public registration requesting ROLE_ADMIN is neutralized to ROLE_PATIENT")
    void testPublicRegistrationPrivilegeEscalationPrevented() {
        when(userRepository.existsByEmail("attacker@carepath.io")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        RegisterRequest request = new RegisterRequest();
        request.setEmail("attacker@carepath.io");
        request.setPassword("SecurePassword123!");
        request.setFirstName("Attacker");
        request.setLastName("User");
        request.setRole(Role.ROLE_ADMIN); // Malicious attempt to elevate privileges

        RegisterResponse response = authService.register(request, "203.0.113.1", "curl/7.68.0");

        assertThat(response.getRole()).isEqualTo(Role.ROLE_PATIENT); // Enforced to ROLE_PATIENT!

        verify(auditLogService, times(1)).recordEvent(
                any(User.class),
                eq("attacker@carepath.io"),
                isNull(),
                eq("USER_REGISTERED"),
                eq("User"),
                any(UUID.class),
                eq("SUCCESS"),
                eq("203.0.113.1"),
                eq("curl/7.68.0"),
                anyMap()
        );
    }

    @Test
    @DisplayName("Failed login with invalid credentials records LOGIN_FAILURE audit event")
    void testFailedLoginRecordsAudit() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest();
        request.setEmail("victim@carepath.io");
        request.setPassword("WrongPassword");

        assertThatThrownBy(() -> authService.login(request, null, "198.51.100.12", "Mozilla/5.0"))
                .isInstanceOf(BadCredentialsException.class);

        verify(auditLogService, times(1)).recordEvent(
                isNull(),
                eq("victim@carepath.io"),
                isNull(),
                eq("LOGIN_FAILURE"),
                eq("User"),
                isNull(),
                eq("FAILURE"),
                eq("198.51.100.12"),
                eq("Mozilla/5.0"),
                anyMap()
        );
    }

    @Test
    @DisplayName("Logout generates LOGOUT audit event when principal is provided")
    void testLogoutRecordsAuditEvent() {
        User user = new User("sarah@carepath.io", "hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        user.setId(UUID.randomUUID());
        UserPrincipal principal = UserPrincipal.create(user);

        authService.logout(null, principal, "127.0.0.1", "Chrome");

        verify(auditLogService, times(1)).recordEvent(
                isNull(),
                eq("sarah@carepath.io"),
                isNull(),
                eq("LOGOUT"),
                eq("User"),
                eq(user.getId()),
                eq("SUCCESS"),
                eq("127.0.0.1"),
                eq("Chrome"),
                anyMap()
        );
    }
}
