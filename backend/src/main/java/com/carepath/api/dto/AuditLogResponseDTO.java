package com.carepath.api.dto;

import com.carepath.domain.models.AuditLog;

import java.time.Instant;
import java.util.UUID;

public class AuditLogResponseDTO {

    private UUID id;
    private UUID actorUserId;
    private String actorEmail;
    private UUID targetPatientId;
    private String actionType;
    private String entityName;
    private UUID entityId;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String details;
    private Instant createdAt;

    public AuditLogResponseDTO() {
    }

    public AuditLogResponseDTO(UUID id, UUID actorUserId, String actorEmail, UUID targetPatientId,
                               String actionType, String entityName, UUID entityId, String ipAddress,
                               String userAgent, String status, String details, Instant createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.targetPatientId = targetPatientId;
        this.actionType = actionType;
        this.entityName = entityName;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = status;
        this.details = details;
        this.createdAt = createdAt;
    }

    public static AuditLogResponseDTO fromEntity(AuditLog log) {
        if (log == null) return null;
        UUID actorId = log.getActorUser() != null ? log.getActorUser().getId() : null;
        return new AuditLogResponseDTO(
                log.getId(),
                actorId,
                log.getActorEmail(),
                log.getTargetPatientId(),
                log.getActionType(),
                log.getEntityName(),
                log.getEntityId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getStatus(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
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
}
