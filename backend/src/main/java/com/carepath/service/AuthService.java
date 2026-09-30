package com.carepath.service;

import com.carepath.api.dto.*;
import com.carepath.api.exception.AccountDisabledException;
import com.carepath.api.exception.DoctorPendingApprovalException;
import com.carepath.api.exception.DoctorRejectedException;
import com.carepath.api.exception.DuplicateEmailException;
import com.carepath.api.exception.DuplicateLicenseException;
import com.carepath.api.exception.InvalidCredentialsException;
import com.carepath.api.exception.InvalidTokenException;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.AccountStatus;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.ClinicianProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.ClinicianProfileRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.security.JwtTokenProvider;
import com.carepath.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final ClinicianProfileRepository clinicianProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    @Autowired
    public AuthService(UserRepository userRepository,
                       ClinicianProfileRepository clinicianProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.clinicianProfileRepository = clinicianProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditLogService = auditLogService;
    }

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       AuditLogService auditLogService) {
        this(userRepository, null, passwordEncoder, authenticationManager, jwtTokenProvider, auditLogService);
    }

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this(userRepository, null, passwordEncoder, authenticationManager, jwtTokenProvider, null);
    }

    public RegisterResponse register(RegisterRequest request) {
        return register(request, "127.0.0.1", null);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, String clientIp, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        // Hardening: Public self-registration only allows ROLE_PATIENT to prevent privilege escalation
        Role assignedRole = Role.ROLE_PATIENT;
        if (request.getRole() != null && request.getRole() != Role.ROLE_PATIENT) {
            log.warn("[SECURITY_ALERT] Public registration requested elevated role '{}'. Enforcing ROLE_PATIENT for email: {}",
                    request.getRole(), normalizedEmail);
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(
                normalizedEmail,
                hashedPassword,
                assignedRole,
                request.getFirstName().trim(),
                request.getLastName().trim()
        );

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    savedUser,
                    savedUser.getEmail(),
                    null,
                    "USER_REGISTERED",
                    "User",
                    savedUser.getId(),
                    "SUCCESS",
                    clientIp,
                    userAgent,
                    Map.of("role", savedUser.getRole().name())
            );
        }

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                "User registered successfully. Please complete intake profile."
        );
    }

    public DoctorRegisterResponse registerDoctor(DoctorRegisterRequest request) {
        return registerDoctor(request, "127.0.0.1", null);
    }

    @Transactional
    public DoctorRegisterResponse registerDoctor(DoctorRegisterRequest request, String clientIp, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        String licenseNumber = request.getLicenseNumber().trim();
        if (clinicianProfileRepository != null && clinicianProfileRepository.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateLicenseException("Medical license number is already registered: " + licenseNumber);
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User doctorUser = new User(
                normalizedEmail,
                hashedPassword,
                Role.ROLE_CLINICIAN,
                request.getFirstName().trim(),
                request.getLastName().trim()
        );
        doctorUser.setStatus(AccountStatus.PENDING);
        doctorUser.setActive(true);

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            doctorUser.setPhone(request.getPhone().trim());
        }

        User savedUser = userRepository.save(doctorUser);

        if (clinicianProfileRepository != null) {
            ClinicianProfile profile = new ClinicianProfile(
                    savedUser,
                    licenseNumber,
                    request.getSpecialization().trim(),
                    request.getHospitalOrganization().trim()
            );
            clinicianProfileRepository.save(profile);
        }

        log.info("Doctor registered with ID: {}, License: {}, Status: PENDING", savedUser.getId(), licenseNumber);

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    savedUser,
                    savedUser.getEmail(),
                    null,
                    "DOCTOR_REGISTER_PENDING",
                    "User",
                    savedUser.getId(),
                    "PENDING",
                    clientIp,
                    userAgent,
                    Map.of(
                            "licenseNumber", licenseNumber,
                            "specialty", request.getSpecialization().trim(),
                            "clinicName", request.getHospitalOrganization().trim(),
                            "status", AccountStatus.PENDING.name()
                    )
            );
        }

        return new DoctorRegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                AccountStatus.PENDING,
                "Your registration request has been submitted for admin approval."
        );
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        return login(request, response, "127.0.0.1", null);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletResponse response, String clientIp, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            if (auditLogService != null) {
                auditLogService.recordEvent(
                        null,
                        normalizedEmail,
                        null,
                        "LOGIN_FAILURE",
                        "User",
                        null,
                        "FAILURE",
                        clientIp,
                        userAgent,
                        Map.of("reason", "Invalid credentials")
                );
            }
            throw ex;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getId()));

        // Role-based status verification
        if (user.getRole() == Role.ROLE_CLINICIAN) {
            if (user.getStatus() == AccountStatus.PENDING) {
                if (auditLogService != null) {
                    auditLogService.recordEvent(
                            user,
                            normalizedEmail,
                            null,
                            "LOGIN_BLOCKED_PENDING",
                            "User",
                            user.getId(),
                            "FAILURE",
                            clientIp,
                            userAgent,
                            Map.of("reason", "Doctor registration pending approval")
                    );
                }
                throw new DoctorPendingApprovalException("Your doctor registration is still pending admin approval. You will be able to access your account once an administrator approves your registration.");
            } else if (user.getStatus() == AccountStatus.REJECTED) {
                if (auditLogService != null) {
                    auditLogService.recordEvent(
                            user,
                            normalizedEmail,
                            null,
                            "LOGIN_BLOCKED_REJECTED",
                            "User",
                            user.getId(),
                            "FAILURE",
                            clientIp,
                            userAgent,
                            Map.of("reason", "Doctor registration rejected")
                    );
                }
                throw new DoctorRejectedException("Your doctor registration request was not approved.");
            }
        }

        if (!user.isActive() || user.getStatus() != AccountStatus.ACTIVE) {
            if (auditLogService != null) {
                auditLogService.recordEvent(
                        user,
                        normalizedEmail,
                        null,
                        "LOGIN_FAILED_DISABLED",
                        "User",
                        user.getId(),
                        "FAILURE",
                        clientIp,
                        userAgent,
                        Map.of("reason", "Account is deactivated")
                );
            }
            throw new AccountDisabledException("User account is disabled");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

        if (response != null) {
            setRefreshTokenCookie(response, refreshToken);
        }

        if (auditLogService != null) {
            auditLogService.recordEvent(
                    user,
                    user.getEmail(),
                    null,
                    "LOGIN_SUCCESS",
                    "User",
                    user.getId(),
                    "SUCCESS",
                    clientIp,
                    userAgent,
                    Map.of("role", user.getRole().name())
            );
        }

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getFirstName(),
                user.getLastName()
        );

        return new AuthResponse(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                userSummary
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("User authentication required");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getId()));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        Claims claims = jwtTokenProvider.getClaimsFromToken(refreshToken);
        String tokenType = claims.get("tokenType", String.class);
        if (!"REFRESH".equals(tokenType)) {
            throw new InvalidTokenException("Supplied token is not a valid refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for refresh token"));

        if (!user.isActive()) {
            throw new AccountDisabledException("User account is disabled");
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

        if (response != null) {
            setRefreshTokenCookie(response, newRefreshToken);
        }

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getFirstName(),
                user.getLastName()
        );

        return new AuthResponse(
                newAccessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                userSummary
        );
    }

    public MessageResponse logout(HttpServletResponse response) {
        return logout(response, null, "127.0.0.1", null);
    }

    public MessageResponse logout(HttpServletResponse response, UserPrincipal principal, String clientIp, String userAgent) {
        if (response != null) {
            ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(false)
                    .path("/api")
                    .maxAge(0)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        }

        if (principal != null && auditLogService != null) {
            auditLogService.recordEvent(
                    null,
                    principal.getUsername(),
                    null,
                    "LOGOUT",
                    "User",
                    principal.getId(),
                    "SUCCESS",
                    clientIp,
                    userAgent,
                    Map.of("action", "User logged out")
            );
        }

        SecurityContextHolder.clearContext();
        return new MessageResponse("Logged out successfully. Note: Access tokens are stateless and expire at the end of their lifetime.");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in HTTPS production
                .path("/api")
                .maxAge(jwtTokenProvider.getRefreshTokenExpirationSeconds())
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
