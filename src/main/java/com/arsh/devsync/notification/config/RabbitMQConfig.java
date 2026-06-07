package com.arsh.devsync.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${devsync.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${devsync.rabbitmq.notification-queue}")
    private String notificationQueueName;

    @Bean
    public TopicExchange devsyncExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(notificationQueueName)
                .build();
    }

    @Bean
    public Binding taskEventsBinding(Queue notificationQueue, TopicExchange devsyncExchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(devsyncExchange)
                .with("task.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}