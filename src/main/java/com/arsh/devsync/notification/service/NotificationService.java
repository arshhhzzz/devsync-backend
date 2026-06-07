package com.arsh.devsync.notification.service;

import com.arsh.devsync.notification.dto.TaskEventPayload;
import com.arsh.devsync.notification.entity.Notification;
import com.arsh.devsync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void handleTaskEvent(TaskEventPayload event) {
        if (event.assigneeId() == null) {
            return;
        }

        Notification notification = Notification.builder()
                .recipientUserId(event.assigneeId())
                .workspaceId(event.workspaceId())
                .projectId(event.projectId())
                .taskId(event.taskId())
                .eventType(event.eventType())
                .title(buildTitle(event))
                .message(buildMessage(event))
                .read(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));

        notification.setRead(true);
    }

    private String buildTitle(TaskEventPayload event) {
        return switch (event.eventType()) {
            case "TASK_CREATED" -> "New task created";
            case "TASK_ASSIGNED" -> "Task assigned to you";
            case "TASK_STATUS_CHANGED" -> "Task status changed";
            default -> "Task update";
        };
    }

    private String buildMessage(TaskEventPayload event) {
        return switch (event.eventType()) {
            case "TASK_CREATED" ->
                    "A new task was created: " + event.taskTitle();

            case "TASK_ASSIGNED" ->
                    "You were assigned to task: " + event.taskTitle();

            case "TASK_STATUS_CHANGED" ->
                    "Task '" + event.taskTitle() + "' changed from " +
                            event.oldStatus() + " to " + event.newStatus();

            default ->
                    "Task updated: " + event.taskTitle();
        };
    }
}