package com.carepath.api.dto;

import com.carepath.domain.enums.ConfidenceLevel;
import com.carepath.domain.enums.RiskCategory;
import com.carepath.domain.models.ReportShare;
import com.carepath.domain.models.RiskAssessment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SharedReportItemDTO {

    private UUID shareId;
    private UUID reportId;
    private UUID patientId;
    private String patientName;
    private Instant reportDate;
    private Instant sharedDate;
    private String status;
    private BigDecimal overallRiskScore;
    private RiskCategory riskCategory;
    private ConfidenceLevel confidenceLevel;
    private String modelVersion;
    private String featureSnapshot;

    public SharedReportItemDTO() {
    }

    public static SharedReportItemDTO fromEntity(ReportShare share) {
        if (share == null) return null;
        RiskAssessment report = share.getReport();
        SharedReportItemDTO dto = new SharedReportItemDTO();
        dto.setShareId(share.getId());
        dto.setReportId(report.getId());
        dto.setPatientId(share.getPatient().getId());

        if (share.getPatient().getUser() != null) {
            dto.setPatientName((share.getPatient().getUser().getFirstName() + " " +
                    share.getPatient().getUser().getLastName()).trim());
        } else {
            dto.setPatientName("Patient " + share.getPatient().getId().toString().substring(0, 8));
        }

        dto.setReportDate(report.getAssessmentTimestamp());
        dto.setSharedDate(share.getSharedAt());
        dto.setStatus(share.getStatus().name());
        dto.setOverallRiskScore(report.getOverallRiskScore());
        dto.setRiskCategory(report.getRiskCategory());
        dto.setConfidenceLevel(report.getConfidenceLevel());
        dto.setModelVersion(report.getModelVersion());
        dto.setFeatureSnapshot(report.getFeatureSnapshot());
        return dto;
    }

    public UUID getShareId() {
        return shareId;
    }

    public void setShareId(UUID shareId) {
        this.shareId = shareId;
    }

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Instant getReportDate() {
        return reportDate;
    }

    public void setReportDate(Instant reportDate) {
        this.reportDate = reportDate;
    }

    public Instant getSharedDate() {
        return sharedDate;
    }

    public void setSharedDate(Instant sharedDate) {
        this.sharedDate = sharedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getFeatureSnapshot() {
        return featureSnapshot;
    }

    public void setFeatureSnapshot(String featureSnapshot) {
        this.featureSnapshot = featureSnapshot;
    }
}
