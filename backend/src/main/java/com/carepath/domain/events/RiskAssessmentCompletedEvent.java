package com.carepath.domain.events;

import java.math.BigDecimal;
import java.util.UUID;

public class RiskAssessmentCompletedEvent {

    private final UUID userId;
    private final UUID patientId;
    private final UUID assessmentId;
    private final String riskCategory;
    private final BigDecimal overallRiskScore;
    private final String eventId;

    public RiskAssessmentCompletedEvent(UUID userId, UUID patientId, UUID assessmentId,
                                        String riskCategory, BigDecimal overallRiskScore, String eventId) {
        this.userId = userId;
        this.patientId = patientId;
        this.assessmentId = assessmentId;
        this.riskCategory = riskCategory;
        this.overallRiskScore = overallRiskScore;
        this.eventId = eventId != null ? eventId : "assessment-" + assessmentId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public BigDecimal getOverallRiskScore() {
        return overallRiskScore;
    }

    public String getEventId() {
        return eventId;
    }
}
