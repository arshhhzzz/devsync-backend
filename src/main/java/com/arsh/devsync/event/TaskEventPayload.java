package com.arsh.devsync.event;

import java.time.Instant;

public record TaskEventPayload(
        EventType eventType,
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