package com.carepath.repository;

import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.Role;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.SymptomLog;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.SymptomRepository;
import com.carepath.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class SymptomRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private SymptomRepository symptomRepository;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        User user = new User(
                "symptom.patient." + UUID.randomUUID() + "@carepath.io",
                "$2a$12$dummyHashValue",
                Role.ROLE_PATIENT,
                "David",
                "Miller"
        );
        user = userRepository.saveAndFlush(user);

        patient = new PatientProfile(
                user,
                LocalDate.of(1982, 11, 8),
                BiologicalSex.MALE,
                new BigDecimal("182.00"),
                new BigDecimal("88.00")
        );
        patient = patientRepository.saveAndFlush(patient);
    }

    @Test
    @DisplayName("Should persist symptom log and retrieve with notes")
    void testSaveAndRetrieveSymptom() {
        SymptomLog log = new SymptomLog(
                patient,
                Instant.now(),
                "FATIGUE",
                6,
                "Mild afternoon lethargy after low carbohydrate lunch."
        );

        SymptomLog saved = symptomRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        List<SymptomLog> list = symptomRepository.findByPatientIdOrderByRecordedAtDesc(patient.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getSymptomType()).isEqualTo("FATIGUE");
        assertThat(list.get(0).getSeverityScore()).isEqualTo(6);
        assertThat(list.get(0).getNotes()).contains("lethargy");
    }

    @Test
    @DisplayName("Should query symptoms within time range")
    void testFindSymptomByTimeRange() {
        Instant now = Instant.now();

        symptomRepository.save(new SymptomLog(patient, now.minus(5, ChronoUnit.DAYS), "HEADACHE", 4, "Tension"));
        symptomRepository.save(new SymptomLog(patient, now.minus(2, ChronoUnit.DAYS), "DIZZINESS", 5, "Standing up quickly"));
        symptomRepository.save(new SymptomLog(patient, now, "FATIGUE", 7, "General exhaustion"));

        List<SymptomLog> filtered = symptomRepository.findByPatientIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                patient.getId(),
                now.minus(3, ChronoUnit.DAYS),
                now.plus(1, ChronoUnit.HOURS)
        );

        assertThat(filtered).hasSize(2);
        assertThat(filtered.get(0).getSymptomType()).isEqualTo("FATIGUE");
        assertThat(filtered.get(1).getSymptomType()).isEqualTo("DIZZINESS");
    }

    @Test
    @DisplayName("Should reject severity score outside 1 to 10 range")
    void testSeverityScoreConstraint() {
        // Severity 15 exceeds maximum of 10
        SymptomLog invalid = new SymptomLog(
                patient,
                Instant.now(),
                "CHEST_DISCOMFORT",
                15,
                "Should fail validation"
        );

        assertThatThrownBy(() -> symptomRepository.saveAndFlush(invalid))
                .isInstanceOf(Exception.class);
    }
}
