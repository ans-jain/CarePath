package com.carepath.domain.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", nullable = true)
    private User actorUser;

    @Size(max = 255)
    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "target_patient_id")
    private UUID targetPatientId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @NotBlank
    @Size(max = 50)
    @Column(name = "entity_name", nullable = false, length = 50)
    private String entityName;

    @Column(name = "entity_id")
    private UUID entityId;

    @NotBlank
    @Size(max = 45)
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @NotBlank
    @Size(max = 20)
    @Column(name = "status", nullable = false, length = 20)
    private String status = "SUCCESS";

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false, columnDefinition = "jsonb")
    private String details = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
    }

    public AuditLog(User actorUser, UUID targetPatientId, String actionType,
                    String entityName, UUID entityId, String ipAddress, String details) {
        this.actorUser = actorUser;
        if (actorUser != null) {
            this.actorEmail = actorUser.getEmail();
        }
        this.targetPatientId = targetPatientId;
        this.actionType = actionType;
        this.entityName = entityName;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.details = details != null ? details : "{}";
        this.status = "SUCCESS";
    }

    public AuditLog(User actorUser, String actorEmail, UUID targetPatientId, String actionType,
                    String entityName, UUID entityId, String ipAddress, String userAgent,
                    String status, String details) {
        this.actorUser = actorUser;
        this.actorEmail = actorEmail != null ? actorEmail : (actorUser != null ? actorUser.getEmail() : null);
        this.targetPatientId = targetPatientId;
        this.actionType = actionType;
        this.entityName = entityName;
        this.entityId = entityId;
        this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
        this.userAgent = userAgent;
        this.status = status != null ? status : "SUCCESS";
        this.details = details != null ? details : "{}";
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.details == null) {
            this.details = "{}";
        }
        if (this.status == null) {
            this.status = "SUCCESS";
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getActorUser() {
        return actorUser;
    }

    public void setActorUser(User actorUser) {
        this.actorUser = actorUser;
        if (actorUser != null && this.actorEmail == null) {
            this.actorEmail = actorUser.getEmail();
        }
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public UUID getTargetPatientId() {
        return targetPatientId;
    }

    public void setTargetPatientId(UUID targetPatientId) {
        this.targetPatientId = targetPatientId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
