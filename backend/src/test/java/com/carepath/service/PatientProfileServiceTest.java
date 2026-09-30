package com.carepath.service;

import com.carepath.api.dto.PatientIntakeRequestDTO;
import com.carepath.api.dto.PatientIntakeResponseDTO;
import com.carepath.api.dto.PatientProfileRequestDTO;
import com.carepath.api.dto.PatientProfileResponseDTO;
import com.carepath.api.exception.InvalidCredentialsException;
import com.carepath.api.exception.ResourceNotFoundException;
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
import com.carepath.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private ObjectMapper objectMapper;
    private PatientProfileService patientProfileService;

    private User sampleUser;
    private User otherUser;
    private User adminUser;
    private UserPrincipal samplePrincipal;
    private UserPrincipal otherPrincipal;
    private UserPrincipal adminPrincipal;
    private PatientProfile sampleProfile;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        patientProfileService = new PatientProfileService(patientRepository, userRepository, auditLogRepository, objectMapper);

        sampleUser = new User("sarah.jenkins@example.com", "$2a$10$hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        sampleUser.setId(UUID.randomUUID());
        samplePrincipal = UserPrincipal.create(sampleUser);

        otherUser = new User("other.patient@example.com", "$2a$10$hash", Role.ROLE_PATIENT, "Bob", "Smith");
        otherUser.setId(UUID.randomUUID());
        otherPrincipal = UserPrincipal.create(otherUser);

        adminUser = new User("admin@carepath.io", "$2a$10$hash", Role.ROLE_ADMIN, "Admin", "User");
        adminUser.setId(UUID.randomUUID());
        adminPrincipal = UserPrincipal.create(adminUser);

        sampleProfile = new PatientProfile(
                sampleUser,
                LocalDate.of(1978, 4, 12),
                BiologicalSex.FEMALE,
                new BigDecimal("165.00"),
                new BigDecimal("72.50")
        );
        sampleProfile.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should accurately calculate BMI")
    void testCalculateBmi() {
        // height: 165 cm, weight: 72.5 kg -> BMI = 72.5 / (1.65)^2 = 26.6299... -> 26.63
        BigDecimal bmi = PatientProfileService.calculateBmi(new BigDecimal("165.00"), new BigDecimal("72.50"));
        assertThat(bmi).isEqualByComparingTo("26.63");

        assertThat(PatientProfileService.calculateBmi(null, new BigDecimal("70"))).isNull();
        assertThat(PatientProfileService.calculateBmi(new BigDecimal("170"), null)).isNull();
        assertThat(PatientProfileService.calculateBmi(BigDecimal.ZERO, new BigDecimal("70"))).isNull();
        assertThat(PatientProfileService.calculateBmi(new BigDecimal("170"), BigDecimal.ZERO)).isNull();
    }

    @Test
    @DisplayName("Should retrieve profile for current user")
    void testGetProfileForCurrentUser() {
        when(patientRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleProfile));

        PatientProfileResponseDTO response = patientProfileService.getProfileForCurrentUser(samplePrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(sampleProfile.getId());
        assertThat(response.getUserId()).isEqualTo(sampleUser.getId());
        assertThat(response.getEmail()).isEqualTo("sarah.jenkins@example.com");
        assertThat(response.getFirstName()).isEqualTo("Sarah");
        assertThat(response.getCurrentBmi()).isEqualByComparingTo("26.63");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when profile does not exist for current user")
    void testGetProfileForCurrentUserNotFound() {
        when(patientRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientProfileService.getProfileForCurrentUser(samplePrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when principal is null")
    void testGetProfileNullPrincipal() {
        assertThatThrownBy(() -> patientProfileService.getProfileForCurrentUser(null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Should allow patient to get profile by ID if they own it")
    void testGetProfileByIdOwner() {
        when(patientRepository.findById(sampleProfile.getId())).thenReturn(Optional.of(sampleProfile));

        PatientProfileResponseDTO response = patientProfileService.getProfileById(sampleProfile.getId(), samplePrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(sampleProfile.getId());
    }

    @Test
    @DisplayName("Should allow admin to view any patient profile by ID")
    void testGetProfileByIdAdmin() {
        when(patientRepository.findById(sampleProfile.getId())).thenReturn(Optional.of(sampleProfile));

        PatientProfileResponseDTO response = patientProfileService.getProfileById(sampleProfile.getId(), adminPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(sampleProfile.getId());
    }

    @Test
    @DisplayName("Should reject access when patient attempts to view another patient's profile")
    void testGetProfileByIdForbidden() {
        when(patientRepository.findById(sampleProfile.getId())).thenReturn(Optional.of(sampleProfile));

        assertThatThrownBy(() -> patientProfileService.getProfileById(sampleProfile.getId(), otherPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Should upsert profile and record audit log")
    void testUpsertProfileForCurrentUser() {
        PatientProfileRequestDTO request = new PatientProfileRequestDTO(
                LocalDate.of(1980, 5, 20),
                BiologicalSex.FEMALE,
                new BigDecimal("170.00"),
                new BigDecimal("68.00"),
                SmokingStatus.NEVER,
                AlcoholUse.OCCASIONAL,
                Map.of("hypertensionHistory", true)
        );
        request.setFirstName("Sarah");
        request.setLastName("Updated");

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(patientRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleProfile));
        when(patientRepository.save(any(PatientProfile.class))).thenAnswer(i -> i.getArgument(0));

        PatientProfileResponseDTO response = patientProfileService.upsertProfileForCurrentUser(request, samplePrincipal, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getHeightCm()).isEqualByComparingTo("170.00");
        assertThat(response.getBaselineWeightKg()).isEqualByComparingTo("68.00");
        assertThat(response.getCurrentBmi()).isEqualByComparingTo("23.53");
        assertThat(response.getMedicalHistory()).containsKey("hypertensionHistory");

        verify(auditLogRepository).save(any(AuditLog.class));
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("Should submit intake successfully and calculate baseline BMI")
    void testSubmitIntake() {
        PatientIntakeRequestDTO request = new PatientIntakeRequestDTO(
                LocalDate.of(1985, 8, 15),
                BiologicalSex.MALE,
                new BigDecimal("180.00"),
                new BigDecimal("80.00"),
                SmokingStatus.FORMER,
                AlcoholUse.MODERATE,
                Map.of("familyHistory", true)
        );

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(patientRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.empty());
        when(patientRepository.save(any(PatientProfile.class))).thenAnswer(i -> {
            PatientProfile p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PatientIntakeResponseDTO response = patientProfileService.submitIntake(request, samplePrincipal, "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getBiologicalSex()).isEqualTo(BiologicalSex.MALE);
        assertThat(response.getHeightCm()).isEqualByComparingTo("180.00");
        assertThat(response.getBaselineWeightKg()).isEqualByComparingTo("80.00");
        assertThat(response.getCurrentBmi()).isEqualByComparingTo("24.69");
        assertThat(response.getMessage()).contains("successfully");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should retrieve intake for current user")
    void testGetIntakeForCurrentUser() {
        when(patientRepository.findByUserId(sampleUser.getId())).thenReturn(Optional.of(sampleProfile));

        PatientIntakeResponseDTO response = patientProfileService.getIntakeForCurrentUser(samplePrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(sampleProfile.getId());
        assertThat(response.getBiologicalSex()).isEqualTo(BiologicalSex.FEMALE);
        assertThat(response.getCurrentBmi()).isEqualByComparingTo("26.63");
    }
}
