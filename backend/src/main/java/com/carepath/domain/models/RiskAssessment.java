package com.carepath.domain.models;

import com.carepath.domain.enums.ConfidenceLevel;
import com.carepath.domain.enums.RiskCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @NotNull
    @Column(name = "assessment_timestamp", nullable = false)
    private Instant assessmentTimestamp;

    @NotBlank
    @Size(max = 32)
    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;

    @NotNull
    @DecimalMin("0.000")
    @DecimalMax("1.000")
    @Column(name = "overall_risk_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal overallRiskScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    private RiskCategory riskCategory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "confidence_level", nullable = false, length = 32)
    private ConfidenceLevel confidenceLevel;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_snapshot", nullable = false, columnDefinition = "jsonb")
    private String featureSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RiskAssessment() {
    }

    public RiskAssessment(PatientProfile patient, Instant assessmentTimestamp, String modelVersion,
                          BigDecimal overallRiskScore, RiskCategory riskCategory,
                          ConfidenceLevel confidenceLevel, String featureSnapshot) {
        this.patient = patient;
        this.assessmentTimestamp = assessmentTimestamp;
        this.modelVersion = modelVersion;
        this.overallRiskScore = overallRiskScore;
        this.riskCategory = riskCategory;
        this.confidenceLevel = confidenceLevel;
        this.featureSnapshot = featureSnapshot;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.assessmentTimestamp == null) {
            this.assessmentTimestamp = this.createdAt;
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PatientProfile getPatient() {
        return patient;
    }

    public void setPatient(PatientProfile patient) {
        this.patient = patient;
    }

    public Instant getAssessmentTimestamp() {
        return assessmentTimestamp;
    }

    public void setAssessmentTimestamp(Instant assessmentTimestamp) {
        this.assessmentTimestamp = assessmentTimestamp;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public BigDecimal getOverallRiskScore() {
        return overallRiskScore;
    }

    public void setOverallRiskScore(BigDecimal overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(RiskCategory riskCategory) {
        this.riskCategory = riskCategory;
    }

    public ConfidenceLevel getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(ConfidenceLevel confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getFeatureSnapshot() {
        return featureSnapshot;
    }

    public void setFeatureSnapshot(String featureSnapshot) {
        this.featureSnapshot = featureSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskAssessment that = (RiskAssessment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
