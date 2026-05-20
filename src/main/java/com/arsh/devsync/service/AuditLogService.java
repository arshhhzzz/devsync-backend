package com.arsh.devsync.service;

import com.arsh.devsync.entity.AuditLog;
import com.arsh.devsync.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(
            Long workspaceId,
            String actorEmail,
            String action,
            String resourceType,
            Long resourceId
    ) {
        AuditLog auditLog = new AuditLog(
                workspaceId,
                actorEmail,
                action,
                resourceType,
                resourceId
        );

        auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAuditLogsByWorkspace(Long workspaceId) {
        return auditLogRepository.findByWorkspaceIdOrderByTimestampDesc(workspaceId);
    }
}