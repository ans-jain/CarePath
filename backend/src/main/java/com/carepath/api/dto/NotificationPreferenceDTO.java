package com.carepath.api.dto;

import com.carepath.domain.models.NotificationPreference;

import java.time.Instant;
import java.util.UUID;

public class NotificationPreferenceDTO {

    private UUID userId;
    private boolean inAppEnabled;
    private boolean emailEnabled;
    private boolean riskAssessmentCompletedEnabled;
    private boolean remindersEnabled;
    private boolean systemUpdatesEnabled;
    private Instant updatedAt;

    public NotificationPreferenceDTO() {
    }

    public static NotificationPreferenceDTO fromEntity(NotificationPreference preference) {
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setUserId(preference.getUser().getId());
        dto.setInAppEnabled(preference.isInAppEnabled());
        dto.setEmailEnabled(preference.isEmailEnabled());
        dto.setRiskAssessmentCompletedEnabled(preference.isRiskAssessmentCompletedEnabled());
        dto.setRemindersEnabled(preference.isRemindersEnabled());
        dto.setSystemUpdatesEnabled(preference.isSystemUpdatesEnabled());
        dto.setUpdatedAt(preference.getUpdatedAt());
        return dto;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isRiskAssessmentCompletedEnabled() {
        return riskAssessmentCompletedEnabled;
    }

    public void setRiskAssessmentCompletedEnabled(boolean riskAssessmentCompletedEnabled) {
        this.riskAssessmentCompletedEnabled = riskAssessmentCompletedEnabled;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public boolean isSystemUpdatesEnabled() {
        return systemUpdatesEnabled;
    }

    public void setSystemUpdatesEnabled(boolean systemUpdatesEnabled) {
        this.systemUpdatesEnabled = systemUpdatesEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
