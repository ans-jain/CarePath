package com.carepath.domain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "shap_explanations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_shap_explanations_risk_assessment_id", columnNames = "risk_assessment_id")
})
public class ShapExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_assessment_id", nullable = false, unique = true)
    private RiskAssessment riskAssessment;

    @NotNull
    @Column(name = "base_value", nullable = false, precision = 6, scale = 4)
    private BigDecimal baseValue;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_contributions", nullable = false, columnDefinition = "jsonb")
    private String featureContributions;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_risk_drivers", nullable = false, columnDefinition = "jsonb")
    private String topRiskDrivers;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_protective_factors", nullable = false, columnDefinition = "jsonb")
    private String topProtectiveFactors;

    @NotBlank
    @Column(name = "clinical_summary_narrative", nullable = false, columnDefinition = "text")
    private String clinicalSummaryNarrative;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ShapExplanation() {
    }

    public ShapExplanation(RiskAssessment riskAssessment, BigDecimal baseValue,
                           String featureContributions, String topRiskDrivers,
                           String topProtectiveFactors, String clinicalSummaryNarrative) {
        this.riskAssessment = riskAssessment;
        this.baseValue = baseValue;
        this.featureContributions = featureContributions;
        this.topRiskDrivers = topRiskDrivers;
        this.topProtectiveFactors = topProtectiveFactors;
        this.clinicalSummaryNarrative = clinicalSummaryNarrative;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }

    public void setRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
    }

    public BigDecimal getBaseValue() {
        return baseValue;
    }

    public void setBaseValue(BigDecimal baseValue) {
        this.baseValue = baseValue;
    }

    public String getFeatureContributions() {
        return featureContributions;
    }

    public void setFeatureContributions(String featureContributions) {
        this.featureContributions = featureContributions;
    }

    public String getTopRiskDrivers() {
        return topRiskDrivers;
    }

    public void setTopRiskDrivers(String topRiskDrivers) {
        this.topRiskDrivers = topRiskDrivers;
    }

    public String getTopProtectiveFactors() {
        return topProtectiveFactors;
    }

    public void setTopProtectiveFactors(String topProtectiveFactors) {
        this.topProtectiveFactors = topProtectiveFactors;
    }

    public String getClinicalSummaryNarrative() {
        return clinicalSummaryNarrative;
    }

    public void setClinicalSummaryNarrative(String clinicalSummaryNarrative) {
        this.clinicalSummaryNarrative = clinicalSummaryNarrative;
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
        ShapExplanation that = (ShapExplanation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
