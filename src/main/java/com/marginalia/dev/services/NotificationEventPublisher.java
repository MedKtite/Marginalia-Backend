package com.marginalia.dev.services;

import com.marginalia.dev.dto.notifications.EmailMessageDto;
import com.marginalia.dev.dto.notifications.PushNotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${marginalia.rabbitmq.exchange:marginalia.exchange}")
    private String exchangeName;

    @Value("${marginalia.rabbitmq.email-routing-key:email.send}")
    private String emailRoutingKey;

    @Value("${marginalia.rabbitmq.push-routing-key:push.send}")
    private String pushRoutingKey;

    public NotificationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishEmail(String to, String subject, String templateName, Map<String, Object> variables, String plainTextContent) {
        EmailMessageDto dto = new EmailMessageDto(to, subject, templateName, variables, plainTextContent);
        try {
            rabbitTemplate.convertAndSend(exchangeName, emailRoutingKey, dto);
            log.info("Published email event to RabbitMQ for: {}", to);
        } catch (Exception e) {
            log.error("Failed to publish email event to RabbitMQ for {}: {}", to, e.getMessage(), e);
        }
    }

    public void publishPush(String token, String title, String body, String category, Map<String, String> data) {
        PushNotificationDto dto = new PushNotificationDto(token, title, body, category, data);
        try {
            rabbitTemplate.convertAndSend(exchangeName, pushRoutingKey, dto);
            log.debug("Published push notification event to RabbitMQ for token prefix: {}",
                    token != null && token.length() > 8 ? token.substring(0, 8) : token);
        } catch (Exception e) {
            log.error("Failed to publish push notification event to RabbitMQ: {}", e.getMessage(), e);
        }
    }
}
