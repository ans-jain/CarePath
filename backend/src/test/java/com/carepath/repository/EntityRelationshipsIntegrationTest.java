package com.carepath.repository;

import com.carepath.domain.enums.*;
import com.carepath.domain.models.*;
import com.carepath.domain.repository.*;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityRelationshipsIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ClinicianProfileRepository clinicianProfileRepository;

    @Autowired
    private PatientClinicianAccessRepository accessRepository;

    @Autowired
    private VitalRepository vitalRepository;

    @Autowired
    private SymptomRepository symptomRepository;

    @Autowired
    private PatientBaselineRepository baselineRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private ShapExplanationRepository shapExplanationRepository;

    @Autowired
    private CounterfactualRecommendationRepository counterfactualRepository;

    @Autowired
    private ClinicalAlertRepository alertRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Should successfully persist and query full domain aggregate graph")
    void testCompleteDomainEntityGraph() {
        // 1. Create Patient User & PatientProfile
        User patientUser = new User(
                "sarah." + UUID.randomUUID() + "@example.com",
                "$2a$12$secureHash123",
                Role.ROLE_PATIENT,
                "Sarah",
                "Jenkins"
        );
        patientUser = userRepository.saveAndFlush(patientUser);

        PatientProfile patient = new PatientProfile(
                patientUser,
                LocalDate.of(1978, 4, 12),
                BiologicalSex.FEMALE,
                new BigDecimal("165.00"),
                new BigDecimal("72.50")
        );
        patient.setSmokingStatus(SmokingStatus.NEVER);
        patient.setAlcoholUse(AlcoholUse.OCCASIONAL);
        patient.setMedicalHistory("{\"hypertensionHistory\": true, \"gestationalDiabetes\": true}");
        patient = patientRepository.saveAndFlush(patient);

        // 2. Create Clinician User & ClinicianProfile
        User clinicianUser = new User(
                "dr.evans." + UUID.randomUUID() + "@hospital.org",
                "$2a$12$secureDoctorHash456",
                Role.ROLE_CLINICIAN,
                "Marcus",
                "Evans"
        );
        clinicianUser = userRepository.saveAndFlush(clinicianUser);

        ClinicianProfile clinician = new ClinicianProfile(
                clinicianUser,
                "MD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Cardiology",
                "Metropolitan Health Clinic"
        );
        clinician = clinicianProfileRepository.saveAndFlush(clinician);

        // 3. Delegate Access (Patient grants access to Clinician)
        PatientClinicianAccess accessGrant = new PatientClinicianAccess(
                patient,
                clinician,
                AccessStatus.ACTIVE
        );
        accessGrant.setGrantCode("CARE-8492-XQ7");
        accessGrant.setGrantedAt(Instant.now());
        accessGrant.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        accessRepository.saveAndFlush(accessGrant);

        List<PatientClinicianAccess> activeGrants = accessRepository
                .findByClinicianIdAndAccessStatus(clinician.getId(), AccessStatus.ACTIVE);
        assertThat(activeGrants).hasSize(1);
        assertThat(activeGrants.get(0).getPatient().getId()).isEqualTo(patient.getId());

        // 4. Log Vital Metrics
        VitalMetric bp = new VitalMetric(
                patient,
                Instant.now(),
                MetricType.SYSTOLIC_BP,
                new BigDecimal("132.00"),
                "mmHg",
                MeasurementContext.RESTING,
                MeasurementSource.MANUAL
        );
        bp = vitalRepository.saveAndFlush(bp);
        assertThat(bp.getId()).isNotNull();

        // 5. Log Symptom
        SymptomLog symptom = new SymptomLog(
                patient,
                Instant.now(),
                "HEADACHE",
                4,
                "Mild forehead pressure in morning"
        );
        symptom = symptomRepository.saveAndFlush(symptom);
        assertThat(symptom.getId()).isNotNull();

        // 6. Record Patient Baseline
        PatientBaseline baseline = new PatientBaseline(
                patient,
                MetricType.SYSTOLIC_BP,
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now(),
                new BigDecimal("122.40"),
                new BigDecimal("121.00"),
                new BigDecimal("5.20"),
                new BigDecimal("118.00"),
                new BigDecimal("125.00"),
                new BigDecimal("124.10"),
                28
        );
        baselineRepository.saveAndFlush(baseline);

        Optional<PatientBaseline> latestBaseline = baselineRepository
                .findFirstByPatientIdAndMetricTypeOrderByWindowEndDesc(patient.getId(), MetricType.SYSTOLIC_BP);
        assertThat(latestBaseline).isPresent();
        assertThat(latestBaseline.get().getMeanValue()).isEqualByComparingTo("122.40");

        // 7. Record Risk Assessment
        RiskAssessment riskAssessment = new RiskAssessment(
                patient,
                Instant.now(),
                "carepath-gbm-v1.0.0",
                new BigDecimal("0.480"),
                RiskCategory.MODERATE,
                ConfidenceLevel.LONGITUDINAL_ROBUST,
                "{\"age\": 48, \"systolic_bp\": 132, \"bmi\": 26.63}"
        );
        riskAssessment = riskAssessmentRepository.saveAndFlush(riskAssessment);

        // 8. Record SHAP Explanation
        ShapExplanation shap = new ShapExplanation(
                riskAssessment,
                new BigDecimal("0.3120"),
                "{\"fasting_glucose_ewma\": 0.085, \"systolic_bp_slope\": 0.062}",
                "[{\"feature\": \"fasting_glucose_ewma\", \"shap\": 0.085}]",
                "[{\"feature\": \"hdl_cholesterol\", \"shap\": -0.050}]",
                "Blood pressure trajectory and glucose are primary drivers."
        );
        shapExplanationRepository.saveAndFlush(shap);

        Optional<ShapExplanation> foundShap = shapExplanationRepository.findByRiskAssessmentId(riskAssessment.getId());
        assertThat(foundShap).isPresent();
        assertThat(foundShap.get().getBaseValue()).isEqualByComparingTo("0.3120");

        // 9. Record Counterfactual Recommendation
        CounterfactualRecommendation counterfactual = new CounterfactualRecommendation(
                riskAssessment,
                new BigDecimal("0.220"),
                "{\"systolic_bp\": 120.0, \"fasting_glucose\": 96.0}",
                new BigDecimal("1.420")
        );
        counterfactualRepository.saveAndFlush(counterfactual);

        List<CounterfactualRecommendation> cfList = counterfactualRepository
                .findByRiskAssessmentId(riskAssessment.getId());
        assertThat(cfList).hasSize(1);
        assertThat(cfList.get(0).getTargetRiskScore()).isEqualByComparingTo("0.220");

        // 10. Trigger Clinical Alert linked to Vital
        ClinicalAlert alert = new ClinicalAlert(
                patient,
                AlertType.SUSTAINED_TREND,
                AlertSeverity.WARNING,
                "Sustained Upward Blood Pressure Drift",
                "Systolic BP trend has been elevating over the past 14 days."
        );
        alert.setTriggerMetric(bp);
        alert = alertRepository.saveAndFlush(alert);

        // Acknowledge alert by clinician
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedByUser(clinicianUser);
        alert = alertRepository.saveAndFlush(alert);

        assertThat(alert.getTriggerMetric().getId()).isEqualTo(bp.getId());
        assertThat(alert.getAcknowledgedByUser().getId()).isEqualTo(clinicianUser.getId());

        // 11. Record Immutable Audit Log
        AuditLog audit = new AuditLog(
                clinicianUser,
                patient.getId(),
                "VIEW_PATIENT_FILE",
                "PatientProfile",
                patient.getId(),
                "192.168.1.100",
                "{\"action\": \"consultation_review\"}"
        );
        audit = auditLogRepository.saveAndFlush(audit);
        assertThat(audit.getId()).isNotNull();
        assertThat(audit.getActionType()).isEqualTo("VIEW_PATIENT_FILE");
    }
}
