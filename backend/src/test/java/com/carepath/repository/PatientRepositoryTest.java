package com.carepath.repository;

import com.carepath.domain.enums.AlcoholUse;
import com.carepath.domain.enums.BiologicalSex;
import com.carepath.domain.enums.Role;
import com.carepath.domain.enums.SmokingStatus;
import com.carepath.domain.models.PatientProfile;
import com.carepath.domain.models.User;
import com.carepath.domain.repository.PatientRepository;
import com.carepath.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PatientRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    private User user;
    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        user = new User(
                "patient." + UUID.randomUUID() + "@carepath.io",
                "$2a$12$dummyHashValue",
                Role.ROLE_PATIENT,
                "Jane",
                "Doe"
        );
        user = userRepository.saveAndFlush(user);

        patientProfile = new PatientProfile(
                user,
                LocalDate.of(1985, 6, 15),
                BiologicalSex.FEMALE,
                new BigDecimal("168.00"),
                new BigDecimal("65.50")
        );
        patientProfile.setSmokingStatus(SmokingStatus.NEVER);
        patientProfile.setAlcoholUse(AlcoholUse.OCCASIONAL);
        patientProfile.setMedicalHistory("{\"hypertension\": false, \"family_diabetes\": true}");
    }

    @Test
    @DisplayName("Should persist and retrieve patient profile with JSONB medical history")
    void testSaveAndFindPatientProfile() {
        PatientProfile saved = patientRepository.save(patientProfile);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getBiologicalSex()).isEqualTo(BiologicalSex.FEMALE);
        assertThat(saved.getHeightCm()).isEqualByComparingTo("168.00");
        assertThat(saved.getBaselineWeightKg()).isEqualByComparingTo("65.50");

        Optional<PatientProfile> byUserId = patientRepository.findByUserId(user.getId());
        assertThat(byUserId).isPresent();
        assertThat(byUserId.get().getMedicalHistory()).contains("family_diabetes");
        assertThat(byUserId.get().getUser().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("Should enforce one-to-one constraint between User and PatientProfile")
    void testOneToOneUserConstraint() {
        patientRepository.saveAndFlush(patientProfile);

        PatientProfile duplicateProfile = new PatientProfile(
                user,
                LocalDate.of(1990, 1, 1),
                BiologicalSex.FEMALE,
                new BigDecimal("160.00"),
                new BigDecimal("55.00")
        );

        assertThatThrownBy(() -> patientRepository.saveAndFlush(duplicateProfile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should enforce height physiological boundary check constraint")
    void testHeightCheckConstraint() {
        // Height below 50 cm must fail
        patientProfile.setHeightCm(new BigDecimal("35.00"));

        assertThatThrownBy(() -> patientRepository.saveAndFlush(patientProfile))
                .isInstanceOf(Exception.class);
    }
}
