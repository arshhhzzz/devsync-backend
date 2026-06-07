package com.arsh.devsync.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${devsync.rabbitmq.exchange}")
    private String exchangeName;

    public void publishTaskEvent(TaskEventPayload payload) {
        String routingKey = switch (payload.eventType()) {
            case TASK_CREATED -> "task.created";
            case TASK_ASSIGNED -> "task.assigned";
            case TASK_STATUS_CHANGED -> "task.status.changed";
        };

        rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);

        log.info("Published event={} taskId={} routingKey={}",
                payload.eventType(), payload.taskId(), routingKey);
    }
}