package com.arsh.devsync.dto;

import com.arsh.devsync.entity.AuditLog;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private Long workspaceId;
    private String actorEmail;
    private String action;
    private String resourceType;
    private Long resourceId;
    private LocalDateTime timestamp;

    public AuditLogResponse(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.workspaceId = auditLog.getWorkspaceId();
        this.actorEmail = auditLog.getActorEmail();
        this.action = auditLog.getAction();
        this.resourceType = auditLog.getResourceType();
        this.resourceId = auditLog.getResourceId();
        this.timestamp = auditLog.getTimestamp();
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}