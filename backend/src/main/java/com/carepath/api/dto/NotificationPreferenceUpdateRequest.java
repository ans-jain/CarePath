package com.carepath.api.dto;

public class NotificationPreferenceUpdateRequest {

    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean riskAssessmentCompletedEnabled;
    private Boolean remindersEnabled;
    private Boolean systemUpdatesEnabled;

    public NotificationPreferenceUpdateRequest() {
    }

    public Boolean getInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(Boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getRiskAssessmentCompletedEnabled() {
        return riskAssessmentCompletedEnabled;
    }

    public void setRiskAssessmentCompletedEnabled(Boolean riskAssessmentCompletedEnabled) {
        this.riskAssessmentCompletedEnabled = riskAssessmentCompletedEnabled;
    }

    public Boolean getRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(Boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public Boolean getSystemUpdatesEnabled() {
        return systemUpdatesEnabled;
    }

    public void setSystemUpdatesEnabled(Boolean systemUpdatesEnabled) {
        this.systemUpdatesEnabled = systemUpdatesEnabled;
    }
}
