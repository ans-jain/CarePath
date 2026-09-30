package com.carepath.api.dto;

import com.carepath.domain.enums.ConfidenceLevel;
import com.carepath.domain.enums.RiskCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RiskAssessmentRecordDTO {

    @NotBlank
    private String modelVersion = "calibrated_v1.0.0";

    @NotNull
    @DecimalMin("0.000")
    @DecimalMax("1.000")
    private BigDecimal overallRiskScore;

    @NotNull
    private RiskCategory riskCategory;

    @NotNull
    private ConfidenceLevel confidenceLevel = ConfidenceLevel.LONGITUDINAL_ROBUST;

    private String featureSnapshot = "{}";

    public RiskAssessmentRecordDTO() {
    }

    public RiskAssessmentRecordDTO(String modelVersion, BigDecimal overallRiskScore,
                                  RiskCategory riskCategory, ConfidenceLevel confidenceLevel, String featureSnapshot) {
        this.modelVersion = modelVersion;
        this.overallRiskScore = overallRiskScore;
        this.riskCategory = riskCategory;
        this.confidenceLevel = confidenceLevel;
        this.featureSnapshot = featureSnapshot != null ? featureSnapshot : "{}";
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
}
