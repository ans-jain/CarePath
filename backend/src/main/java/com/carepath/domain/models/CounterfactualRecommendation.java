package com.carepath.domain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "counterfactual_recommendations")
public class CounterfactualRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_assessment_id", nullable = false)
    private RiskAssessment riskAssessment;

    @NotNull
    @DecimalMin("0.000")
    @DecimalMax("1.000")
    @Column(name = "target_risk_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal targetRiskScore;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "simulated_features", nullable = false, columnDefinition = "jsonb")
    private String simulatedFeatures;

    @NotNull
    @Column(name = "feasibility_distance", nullable = false, precision = 6, scale = 3)
    private BigDecimal feasibilityDistance;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public CounterfactualRecommendation() {
    }

    public CounterfactualRecommendation(RiskAssessment riskAssessment, BigDecimal targetRiskScore,
                                        String simulatedFeatures, BigDecimal feasibilityDistance) {
        this.riskAssessment = riskAssessment;
        this.targetRiskScore = targetRiskScore;
        this.simulatedFeatures = simulatedFeatures;
        this.feasibilityDistance = feasibilityDistance;
    }

    @PrePersist
    protected void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = Instant.now();
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

    public BigDecimal getTargetRiskScore() {
        return targetRiskScore;
    }

    public void setTargetRiskScore(BigDecimal targetRiskScore) {
        this.targetRiskScore = targetRiskScore;
    }

    public String getSimulatedFeatures() {
        return simulatedFeatures;
    }

    public void setSimulatedFeatures(String simulatedFeatures) {
        this.simulatedFeatures = simulatedFeatures;
    }

    public BigDecimal getFeasibilityDistance() {
        return feasibilityDistance;
    }

    public void setFeasibilityDistance(BigDecimal feasibilityDistance) {
        this.feasibilityDistance = feasibilityDistance;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CounterfactualRecommendation that = (CounterfactualRecommendation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
