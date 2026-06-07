package com.arsh.devsync.notification.dto;

import java.time.Instant;

public record TaskEventPayload(
        String eventType,
        Long workspaceId,
        Long projectId,
        Long taskId,
        String taskTitle,
        Long actorId,
        Long assigneeId,
        String oldStatus,
        String newStatus,
        Instant occurredAt
) {
}