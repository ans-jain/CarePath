package com.carepath.domain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_preferences_user_id", columnNames = "user_id")
}, indexes = {
        @Index(name = "idx_notification_prefs_user", columnList = "user_id")
})
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "risk_assessment_completed_enabled", nullable = false)
    private boolean riskAssessmentCompletedEnabled = true;

    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled = true;

    @Column(name = "system_updates_enabled", nullable = false)
    private boolean systemUpdatesEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
        this.inAppEnabled = true;
        this.emailEnabled = true;
        this.riskAssessmentCompletedEnabled = true;
        this.remindersEnabled = true;
        this.systemUpdatesEnabled = true;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationPreference that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "NotificationPreference{" +
                "id=" + id +
                ", inAppEnabled=" + inAppEnabled +
                ", emailEnabled=" + emailEnabled +
                ", riskAssessmentCompletedEnabled=" + riskAssessmentCompletedEnabled +
                ", remindersEnabled=" + remindersEnabled +
                ", systemUpdatesEnabled=" + systemUpdatesEnabled +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
