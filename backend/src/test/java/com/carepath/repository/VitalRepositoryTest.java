package com.carepath.repository;

import com.carepath.domain.enums.*;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.models.VitalMetric;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.UserRepository;
import com.carepath.domain.repository.VitalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VitalRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VitalRepository vitalRepository;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        User user = new User(
                "vitals.patient." + UUID.randomUUID() + "@carepath.io",
                "$2a$12$dummyHashValue",
                Role.ROLE_PATIENT,
                "Robert",
                "Taylor"
        );
        user = userRepository.saveAndFlush(user);

        patient = new PatientProfile(
                user,
                LocalDate.of(1975, 3, 20),
                BiologicalSex.MALE,
                new BigDecimal("178.00"),
                new BigDecimal("82.00")
        );
        patient = patientRepository.saveAndFlush(patient);
    }

    @Test
    @DisplayName("Should persist vital metric and retrieve with descending timestamp ordering")
    void testSaveAndRetrieveVitals() {
        Instant now = Instant.now();

        VitalMetric v1 = new VitalMetric(
                patient,
                now.minus(2, ChronoUnit.HOURS),
                MetricType.SYSTOLIC_BP,
                new BigDecimal("124.00"),
                "mmHg",
                MeasurementContext.RESTING,
                MeasurementSource.MANUAL
        );

        VitalMetric v2 = new VitalMetric(
                patient,
                now.minus(1, ChronoUnit.HOURS),
                MetricType.SYSTOLIC_BP,
                new BigDecimal("128.00"),
                "mmHg",
                MeasurementContext.RESTING,
                MeasurementSource.MANUAL
        );

        vitalRepository.save(v1);
        vitalRepository.save(v2);

        List<VitalMetric> list = vitalRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId());
        assertThat(list).hasSize(2);
        // Latest first
        assertThat(list.get(0).getValue()).isEqualByComparingTo("128.00");
        assertThat(list.get(1).getValue()).isEqualByComparingTo("124.00");
    }

    @Test
    @DisplayName("Should query vitals by type and time range")
    void testFindByTypeAndTimeRange() {
        Instant t0 = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant t1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1, ChronoUnit.DAYS);

        vitalRepository.save(new VitalMetric(patient, t0, MetricType.FASTING_GLUCOSE,
                new BigDecimal("95.00"), "mg/dL", MeasurementContext.FASTING, MeasurementSource.MANUAL));
        vitalRepository.save(new VitalMetric(patient, t1, MetricType.FASTING_GLUCOSE,
                new BigDecimal("98.00"), "mg/dL", MeasurementContext.FASTING, MeasurementSource.MANUAL));
        vitalRepository.save(new VitalMetric(patient, t2, MetricType.HEART_RATE,
                new BigDecimal("72.00"), "bpm", MeasurementContext.RESTING, MeasurementSource.MANUAL));

        List<VitalMetric> glucoseRange = vitalRepository.findByPatientIdAndMetricTypeAndRecordedAtBetweenOrderByRecordedAtAsc(
                patient.getId(),
                MetricType.FASTING_GLUCOSE,
                t0.minus(1, ChronoUnit.HOURS),
                t1.plus(1, ChronoUnit.HOURS)
        );

        assertThat(glucoseRange).hasSize(2);
        assertThat(glucoseRange.get(0).getValue()).isEqualByComparingTo("95.00");
        assertThat(glucoseRange.get(1).getValue()).isEqualByComparingTo("98.00");
    }

    @Test
    @DisplayName("Should paginate patient vitals correctly")
    void testPaginatedVitals() {
        Instant base = Instant.now();
        for (int i = 0; i < 15; i++) {
            vitalRepository.save(new VitalMetric(
                    patient,
                    base.minus(i, ChronoUnit.HOURS),
                    MetricType.HEART_RATE,
                    new BigDecimal(60 + i),
                    "bpm",
                    MeasurementContext.RESTING,
                    MeasurementSource.MANUAL
            ));
        }

        Page<VitalMetric> page = vitalRepository.findByPatientIdOrderByRecordedAtDesc(
                patient.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(15);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should enforce positive value check constraint")
    void testPositiveValueConstraint() {
        VitalMetric invalid = new VitalMetric(
                patient,
                Instant.now(),
                MetricType.HEART_RATE,
                new BigDecimal("-5.00"),
                "bpm",
                MeasurementContext.RESTING,
                MeasurementSource.MANUAL
        );

        assertThatThrownBy(() -> vitalRepository.saveAndFlush(invalid))
                .isInstanceOf(Exception.class);
    }
}
