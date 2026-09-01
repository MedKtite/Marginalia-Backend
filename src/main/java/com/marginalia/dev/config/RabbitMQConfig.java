package com.marginalia.dev.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${marginalia.rabbitmq.exchange:marginalia.exchange}")
    private String exchangeName;

    @Value("${marginalia.rabbitmq.email-routing-key:email.send}")
    private String emailRoutingKey;

    @Value("${marginalia.rabbitmq.push-routing-key:push.send}")
    private String pushRoutingKey;

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable("marginalia.email.queue").build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(emailQueue).to(directExchange).with(emailRoutingKey);
    }

    @Bean
    public Queue pushQueue() {
        return QueueBuilder.durable("marginalia.push.queue").build();
    }

    @Bean
    public Binding pushBinding(Queue pushQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(pushQueue).to(directExchange).with(pushRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
