package com.arsh.devsync.notification.consumer;

import com.arsh.devsync.notification.dto.TaskEventPayload;
import com.arsh.devsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${devsync.rabbitmq.notification-queue}")
    public void consume(TaskEventPayload event) {
        log.info(
                "Consumed task event: eventType={}, workspaceId={}, projectId={}, taskId={}, assigneeId={}",
                event.eventType(),
                event.workspaceId(),
                event.projectId(),
                event.taskId(),
                event.assigneeId()
        );

        notificationService.handleTaskEvent(event);
    }
}