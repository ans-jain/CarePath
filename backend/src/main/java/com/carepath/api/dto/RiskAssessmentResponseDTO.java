package com.carepath.api.dto;

import com.carepath.domain.enums.ConfidenceLevel;
import com.carepath.domain.enums.RiskCategory;
import com.carepath.domain.models.RiskAssessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class RiskAssessmentResponseDTO {

    private UUID id;
    private UUID patientId;
    private Instant assessmentTimestamp;
    private String modelVersion;
    private BigDecimal overallRiskScore;
    private RiskCategory riskCategory;
    private ConfidenceLevel confidenceLevel;
    private String featureSnapshot;

    public RiskAssessmentResponseDTO() {
    }

    public static RiskAssessmentResponseDTO fromEntity(RiskAssessment assessment) {
        RiskAssessmentResponseDTO dto = new RiskAssessmentResponseDTO();
        dto.setId(assessment.getId());
        dto.setPatientId(assessment.getPatient().getId());
        dto.setAssessmentTimestamp(assessment.getAssessmentTimestamp());
        dto.setModelVersion(assessment.getModelVersion());
        dto.setOverallRiskScore(assessment.getOverallRiskScore());
        dto.setRiskCategory(assessment.getRiskCategory());
        dto.setConfidenceLevel(assessment.getConfidenceLevel());
        dto.setFeatureSnapshot(assessment.getFeatureSnapshot());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
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
}
