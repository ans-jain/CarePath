package com.carepath.service;

import com.carepath.api.dto.*;
import com.carepath.api.exception.AccountDisabledException;
import com.carepath.api.exception.DuplicateEmailException;
import com.carepath.api.exception.InvalidCredentialsException;
import com.carepath.api.exception.InvalidTokenException;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.JwtTokenProvider;
import com.carepath.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should successfully register a new user and encode password")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "Sarah.Jenkins@Example.COM",
                "SecurePassword123!",
                "Sarah",
                "Jenkins",
                "+15551234567",
                Role.ROLE_PATIENT
        );

        when(userRepository.existsByEmail("sarah.jenkins@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("$2a$10$encodedHash");

        User savedUser = new User("sarah.jenkins@example.com", "$2a$10$encodedHash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(savedUser.getId());
        assertThat(response.getEmail()).isEqualTo("sarah.jenkins@example.com");
        assertThat(response.getRole()).isEqualTo(Role.ROLE_PATIENT);
        assertThat(response.getMessage()).contains("successfully");

        verify(passwordEncoder).encode("SecurePassword123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject duplicate email registration with DuplicateEmailException")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "sarah.jenkins@example.com",
                "SecurePassword123!",
                "Sarah",
                "Jenkins"
        );

        when(userRepository.existsByEmail("sarah.jenkins@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should assign default ROLE_PATIENT when role is null")
    void testRegisterDefaultRole() {
        RegisterRequest request = new RegisterRequest(
                "john.doe@example.com",
                "SecurePassword123!",
                "John",
                "Doe"
        );

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("$2a$10$encodedHash");

        User savedUser = new User("john.doe@example.com", "$2a$10$encodedHash", Role.ROLE_PATIENT, "John", "Doe");
        savedUser.setId(UUID.randomUUID());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(request);

        assertThat(response.getRole()).isEqualTo(Role.ROLE_PATIENT);
    }

    @Test
    @DisplayName("Should successfully login and return access token and user info")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("sarah.jenkins@example.com", "SecurePassword123!");
        UUID userId = UUID.randomUUID();

        User user = new User("sarah.jenkins@example.com", "hashedPassword", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        user.setId(userId);
        UserPrincipal principal = UserPrincipal.create(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("mockAccessToken");
        when(jwtTokenProvider.generateRefreshToken(principal)).thenReturn("mockRefreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse authResponse = authService.login(request, response);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.getAccessToken()).isEqualTo("mockAccessToken");
        assertThat(authResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(authResponse.getExpiresInSeconds()).isEqualTo(900L);
        assertThat(authResponse.getUser().getEmail()).isEqualTo("sarah.jenkins@example.com");
        assertThat(authResponse.getUser().getFirstName()).isEqualTo("Sarah");

        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=mockRefreshToken");
        assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly");
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when login authentication fails")
    void testLoginInvalidCredentials() {
        LoginRequest request = new LoginRequest("sarah.jenkins@example.com", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should throw AccountDisabledException when user account is inactive")
    void testLoginDisabledAccount() {
        LoginRequest request = new LoginRequest("disabled@example.com", "SecurePassword123!");
        UUID userId = UUID.randomUUID();

        User disabledUser = new User("disabled@example.com", "hashedPassword", Role.ROLE_PATIENT, "Disabled", "User");
        disabledUser.setId(userId);
        disabledUser.setActive(false);
        UserPrincipal principal = UserPrincipal.create(disabledUser);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findById(userId)).thenReturn(Optional.of(disabledUser));

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> authService.login(request, response))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("Should return safe user details for getCurrentUser")
    void testGetCurrentUser() {
        UUID userId = UUID.randomUUID();
        User user = new User("sarah.jenkins@example.com", "hashedPassword", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        user.setId(userId);
        user.setPhone("+15551234567");
        UserPrincipal principal = UserPrincipal.create(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse userResponse = authService.getCurrentUser(principal);

        assertThat(userResponse).isNotNull();
        assertThat(userResponse.getId()).isEqualTo(userId);
        assertThat(userResponse.getEmail()).isEqualTo("sarah.jenkins@example.com");
        assertThat(userResponse.getFirstName()).isEqualTo("Sarah");
        assertThat(userResponse.getLastName()).isEqualTo("Jenkins");
        assertThat(userResponse.getPhone()).isEqualTo("+15551234567");
        assertThat(userResponse.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when principal is null for getCurrentUser")
    void testGetCurrentUserNullPrincipal() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should refresh token with valid refresh token")
    void testRefreshTokenSuccess() {
        String refreshToken = "validRefreshToken";
        UUID userId = UUID.randomUUID();

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
        when(jwtTokenProvider.getClaimsFromToken(refreshToken)).thenReturn(claims);
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);

        User user = new User("sarah.jenkins@example.com", "hashedPassword", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(any(UserPrincipal.class))).thenReturn("newRefreshToken");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse authResponse = authService.refreshToken(refreshToken, response);

        assertThat(authResponse.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=newRefreshToken");
    }

    @Test
    @DisplayName("Should reject invalid refresh token")
    void testRefreshTokenInvalid() {
        when(jwtTokenProvider.validateToken("invalidToken")).thenReturn(false);

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> authService.refreshToken("invalidToken", response))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Should logout by clearing cookie and returning message")
    void testLogout() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MessageResponse messageResponse = authService.logout(response);

        assertThat(messageResponse.getMessage()).contains("Logged out");
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
