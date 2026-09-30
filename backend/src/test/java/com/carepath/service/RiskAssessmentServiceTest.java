package com.carepath.service;

import com.carepath.api.dto.RiskAssessmentRecordDTO;
import com.carepath.api.dto.RiskAssessmentResponseDTO;
import com.carepath.domain.enums.ConfidenceLevel;
import com.carepath.domain.enums.RiskCategory;
import com.carepath.domain.enums.Role;
import com.carepath.domain.events.RiskAssessmentCompletedEvent;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.RiskAssessment;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.RiskAssessmentRepository;
import com.carepath.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RiskAssessmentService riskAssessmentService;

    private User sampleUser;
    private PatientProfile samplePatient;
    private UserPrincipal samplePrincipal;
    private UUID userId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        sampleUser = new User("patient@carepath.io", "hash", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(sampleUser, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        samplePatient = new PatientProfile(sampleUser, LocalDate.of(1990, 5, 15),
                com.carepath.domain.enums.BiologicalSex.FEMALE, new BigDecimal("168.0"), new BigDecimal("65.0"));
        try {
            java.lang.reflect.Field idField = PatientProfile.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(samplePatient, patientId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        samplePrincipal = UserPrincipal.create(sampleUser);
        riskAssessmentService = new RiskAssessmentService(riskAssessmentRepository, patientRepository, eventPublisher);
    }

    @Test
    @DisplayName("recordCompletedAssessment saves assessment and publishes RiskAssessmentCompletedEvent")
    void recordCompletedAssessment_Success() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(samplePatient));
        when(riskAssessmentRepository.save(any(RiskAssessment.class))).thenAnswer(invocation -> {
            RiskAssessment a = invocation.getArgument(0);
            try {
                java.lang.reflect.Field idField = RiskAssessment.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(a, UUID.randomUUID());
            } catch (Exception ignored) {}
            return a;
        });

        RiskAssessmentRecordDTO request = new RiskAssessmentRecordDTO(
                "calibrated_v1.0.0",
                new BigDecimal("0.720"),
                RiskCategory.ELEVATED,
                ConfidenceLevel.LONGITUDINAL_ROBUST,
                "{}"
        );

        RiskAssessmentResponseDTO response = riskAssessmentService.recordCompletedAssessment(patientId, request, samplePrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getOverallRiskScore()).isEqualByComparingTo("0.720");
        assertThat(response.getRiskCategory()).isEqualTo(RiskCategory.ELEVATED);

        ArgumentCaptor<RiskAssessmentCompletedEvent> captor = ArgumentCaptor.forClass(RiskAssessmentCompletedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        RiskAssessmentCompletedEvent event = captor.getValue();
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getPatientId()).isEqualTo(patientId);
        assertThat(event.getRiskCategory()).isEqualTo("ELEVATED");
        assertThat(event.getOverallRiskScore()).isEqualByComparingTo("0.720");
    }

    @Test
    @DisplayName("recordCompletedAssessment throws AccessDeniedException when unprivileged user accesses another patient")
    void recordCompletedAssessment_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = new User("other@carepath.io", "hash", Role.ROLE_PATIENT, "Other", "Patient");
        try {
            java.lang.reflect.Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(otherUser, otherUserId);
        } catch (Exception ignored) {}
        UserPrincipal otherPrincipal = UserPrincipal.create(otherUser);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(samplePatient));

        RiskAssessmentRecordDTO request = new RiskAssessmentRecordDTO(
                "calibrated_v1.0.0",
                new BigDecimal("0.350"),
                RiskCategory.MODERATE,
                ConfidenceLevel.PRELIMINARY_INTAKE,
                "{}"
        );

        assertThatThrownBy(() -> riskAssessmentService.recordCompletedAssessment(patientId, request, otherPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission");

        verify(riskAssessmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("getAssessmentHistory returns paged assessments for patient")
    void getAssessmentHistory_Success() {
        RiskAssessment assessment = new RiskAssessment(
                samplePatient,
                java.time.Instant.now(),
                "calibrated_v1.0.0",
                new BigDecimal("0.420"),
                RiskCategory.MODERATE,
                ConfidenceLevel.LONGITUDINAL_ROBUST,
                "{\"systolic\":140}"
        );
        try {
            java.lang.reflect.Field idField = RiskAssessment.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(assessment, UUID.randomUUID());
        } catch (Exception ignored) {}

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<RiskAssessment> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(assessment));

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(samplePatient));
        when(riskAssessmentRepository.findByPatientIdOrderByAssessmentTimestampDesc(patientId, pageable)).thenReturn(page);

        org.springframework.data.domain.Page<RiskAssessmentResponseDTO> result =
                riskAssessmentService.getAssessmentHistory(patientId, pageable, samplePrincipal);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOverallRiskScore()).isEqualByComparingTo("0.420");
        assertThat(result.getContent().get(0).getRiskCategory()).isEqualTo(RiskCategory.MODERATE);
        assertThat(result.getContent().get(0).getFeatureSnapshot()).isEqualTo("{\"systolic\":140}");
    }
}
