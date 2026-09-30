package com.carepath.service;

import com.carepath.api.dto.BatchCreateVitalRequestDTO;
import com.carepath.api.dto.BatchVitalEntryDTO;
import com.carepath.api.dto.CreateVitalRequestDTO;
import com.carepath.api.dto.VitalPageResponseDTO;
import com.carepath.api.dto.VitalResponseDTO;
import com.carepath.api.exception.InvalidMeasurementException;
import com.carepath.api.exception.ResourceNotFoundException;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.MeasurementContext;
import com.carepath.domain.enums.MeasurementSource;
import com.carepath.domain.enums.MetricType;
import com.carepath.domain.enums.Role;
import com.carepath.domain.events.VitalMetricLoggedEvent;
import com.carepath.domain.models.AuditLog;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.models.VitalMetric;
import com.carepath.domain.repository.AuditLogRepository;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.VitalRepository;
import com.carepath.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VitalMetricServiceTest {

    @Mock
    private VitalRepository vitalRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PhysiologicalRangeValidator rangeValidator;
    private VitalMetricService vitalMetricService;

    private User userA;
    private User userB;
    private User adminUser;
    private UserPrincipal principalA;
    private UserPrincipal principalB;
    private UserPrincipal adminPrincipal;
    private PatientProfile profileA;
    private PatientProfile profileB;

    @BeforeEach
    void setUp() {
        rangeValidator = new PhysiologicalRangeValidator();
        vitalMetricService = new VitalMetricService(
                vitalRepository,
                patientRepository,
                auditLogRepository,
                rangeValidator,
                eventPublisher
        );

        userA = new User("sarah@carepath.io", "pass", Role.ROLE_PATIENT, "Sarah", "Jenkins");
        userA.setId(UUID.randomUUID());
        principalA = UserPrincipal.create(userA);

        userB = new User("bob@carepath.io", "pass", Role.ROLE_PATIENT, "Bob", "Smith");
        userB.setId(UUID.randomUUID());
        principalB = UserPrincipal.create(userB);

        adminUser = new User("admin@carepath.io", "pass", Role.ROLE_ADMIN, "Admin", "User");
        adminUser.setId(UUID.randomUUID());
        adminPrincipal = UserPrincipal.create(adminUser);

        profileA = new PatientProfile(userA, LocalDate.of(1985, 5, 20), BiologicalSex.FEMALE, new BigDecimal("170"), new BigDecimal("70"));
        profileA.setId(UUID.randomUUID());

        profileB = new PatientProfile(userB, LocalDate.of(1990, 8, 15), BiologicalSex.MALE, new BigDecimal("180"), new BigDecimal("80"));
        profileB.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should successfully record a single vital metric with canonical defaults")
    void testRecordVitalSuccess() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.SYSTOLIC_BP);
        request.setValue(new BigDecimal("128.00"));
        // unit, context, source omitted -> should get defaults

        when(vitalRepository.save(any(VitalMetric.class))).thenAnswer(invocation -> {
            VitalMetric v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        VitalResponseDTO response = vitalMetricService.recordVital(request, null, principalA, "192.168.1.1");

        assertThat(response).isNotNull();
        assertThat(response.getMetricType()).isEqualTo(MetricType.SYSTOLIC_BP);
        assertThat(response.getValue()).isEqualByComparingTo("128.00");
        assertThat(response.getUnit()).isEqualTo("mmHg");
        assertThat(response.getMeasurementContext()).isEqualTo(MeasurementContext.RESTING);
        assertThat(response.getSource()).isEqualTo(MeasurementSource.MANUAL);
        assertThat(response.getPatientId()).isEqualTo(profileA.getId());

        // Verify event published
        verify(eventPublisher, times(1)).publishEvent(any(VitalMetricLoggedEvent.class));
        // Verify audit log recorded
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should reject physiologically impossible value with InvalidMeasurementException")
    void testRecordVitalPhysiologicalViolation() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.SYSTOLIC_BP);
        request.setValue(new BigDecimal("350.00")); // Above max 300.0 mmHg

        assertThatThrownBy(() -> vitalMetricService.recordVital(request, null, principalA, "127.0.0.1"))
                .isInstanceOf(InvalidMeasurementException.class)
                .hasMessageContaining("Physiological range violation: Systolic blood pressure");

        verify(vitalRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should reject timestamp in the future beyond allowable skew")
    void testRecordVitalFutureTimestamp() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("72.00"));
        request.setRecordedAt(Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> vitalMetricService.recordVital(request, null, principalA, "127.0.0.1"))
                .isInstanceOf(InvalidMeasurementException.class)
                .hasMessageContaining("future");

        verify(vitalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject cross-patient recording attempt with AccessDeniedException")
    void testRecordVitalCrossPatientForbidden() {
        when(patientRepository.findById(profileB.getId())).thenReturn(Optional.of(profileB));

        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("75.00"));

        // User A tries to record vital for Patient B
        assertThatThrownBy(() -> vitalMetricService.recordVital(request, profileB.getId(), principalA, "127.0.0.1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("permission");

        verify(vitalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow Admin to record vitals for any patient")
    void testRecordVitalAdminAllowed() {
        when(patientRepository.findById(profileA.getId())).thenReturn(Optional.of(profileA));
        when(vitalRepository.save(any(VitalMetric.class))).thenAnswer(invocation -> {
            VitalMetric v = invocation.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        CreateVitalRequestDTO request = new CreateVitalRequestDTO();
        request.setMetricType(MetricType.HEART_RATE);
        request.setValue(new BigDecimal("75.00"));

        VitalResponseDTO response = vitalMetricService.recordVital(request, profileA.getId(), adminPrincipal, "127.0.0.1");
        assertThat(response).isNotNull();
        assertThat(response.getMetricType()).isEqualTo(MetricType.HEART_RATE);
    }

    @Test
    @DisplayName("Should successfully record batch vitals atomically")
    void testRecordBatchVitalsSuccess() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        BatchVitalEntryDTO e1 = new BatchVitalEntryDTO(MetricType.SYSTOLIC_BP, new BigDecimal("120.00"), "mmHg");
        BatchVitalEntryDTO e2 = new BatchVitalEntryDTO(MetricType.DIASTOLIC_BP, new BigDecimal("80.00"), "mmHg");
        BatchVitalEntryDTO e3 = new BatchVitalEntryDTO(MetricType.HEART_RATE, new BigDecimal("70.00"), "bpm");

        BatchCreateVitalRequestDTO request = new BatchCreateVitalRequestDTO(Instant.now(), List.of(e1, e2, e3));

        when(vitalRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<VitalMetric> list = invocation.getArgument(0);
            list.forEach(v -> v.setId(UUID.randomUUID()));
            return list;
        });

        List<VitalResponseDTO> responses = vitalMetricService.recordBatchVitals(request, null, principalA, "127.0.0.1");

        assertThat(responses).hasSize(3);
        verify(vitalRepository, times(1)).saveAll(anyList());
        verify(eventPublisher, times(3)).publishEvent(any(VitalMetricLoggedEvent.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should reject entire batch if one entry has physiological violation")
    void testRecordBatchVitalsFailureAtomicity() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        BatchVitalEntryDTO e1 = new BatchVitalEntryDTO(MetricType.SYSTOLIC_BP, new BigDecimal("120.00"), "mmHg");
        BatchVitalEntryDTO e2 = new BatchVitalEntryDTO(MetricType.SPO2, new BigDecimal("150.00"), "%"); // Invalid SpO2 (>100)

        BatchCreateVitalRequestDTO request = new BatchCreateVitalRequestDTO(Instant.now(), List.of(e1, e2));

        assertThatThrownBy(() -> vitalMetricService.recordBatchVitals(request, null, principalA, "127.0.0.1"))
                .isInstanceOf(InvalidMeasurementException.class)
                .hasMessageContaining("SpO2");

        verify(vitalRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should query longitudinal vitals with pagination")
    void testGetVitalsLongitudinal() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        VitalMetric v1 = new VitalMetric(profileA, Instant.now().minus(2, ChronoUnit.HOURS), MetricType.HEART_RATE,
                new BigDecimal("72"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        v1.setId(UUID.randomUUID());

        VitalMetric v2 = new VitalMetric(profileA, Instant.now().minus(1, ChronoUnit.HOURS), MetricType.HEART_RATE,
                new BigDecimal("75"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        v2.setId(UUID.randomUUID());

        Page<VitalMetric> page = new PageImpl<>(List.of(v2, v1), PageRequest.of(0, 10), 2);
        when(vitalRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        VitalPageResponseDTO result = vitalMetricService.getVitals(
                null,
                MetricType.HEART_RATE,
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now(),
                PageRequest.of(0, 10),
                principalA
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getPage()).isZero();
    }

    @Test
    @DisplayName("Should get latest vitals per metric type")
    void testGetLatestVitals() {
        when(patientRepository.findByUserId(userA.getId())).thenReturn(Optional.of(profileA));

        VitalMetric vBp = new VitalMetric(profileA, Instant.now(), MetricType.SYSTOLIC_BP,
                new BigDecimal("124"), "mmHg", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        vBp.setId(UUID.randomUUID());

        VitalMetric vHr = new VitalMetric(profileA, Instant.now(), MetricType.HEART_RATE,
                new BigDecimal("68"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL);
        vHr.setId(UUID.randomUUID());

        when(vitalRepository.findLatestPerMetricType(profileA.getId())).thenReturn(List.of(vBp, vHr));

        List<VitalResponseDTO> latest = vitalMetricService.getLatestVitals(null, null, principalA);

        assertThat(latest).hasSize(2);
        assertThat(latest).extracting(VitalResponseDTO::getMetricType)
                .containsExactlyInAnyOrder(MetricType.SYSTOLIC_BP, MetricType.HEART_RATE);
    }

    @Test
    @DisplayName("Should retrieve vital by ID successfully")
    void testGetVitalByIdSuccess() {
        UUID vitalId = UUID.randomUUID();
        VitalMetric vital = new VitalMetric(profileA, Instant.now(), MetricType.FASTING_GLUCOSE,
                new BigDecimal("95"), "mg/dL", MeasurementContext.FASTING, MeasurementSource.MANUAL);
        vital.setId(vitalId);

        when(vitalRepository.findById(vitalId)).thenReturn(Optional.of(vital));

        VitalResponseDTO response = vitalMetricService.getVitalById(vitalId, null, principalA);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(vitalId);
        assertThat(response.getMetricType()).isEqualTo(MetricType.FASTING_GLUCOSE);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when vital ID does not exist")
    void testGetVitalByIdNotFound() {
        UUID vitalId = UUID.randomUUID();
        when(vitalRepository.findById(vitalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vitalMetricService.getVitalById(vitalId, null, principalA))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
