package com.possystem.config;

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

    @Value("${rabbitmq.exchange.name:pos.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.email:pos.email.queue}")
    private String emailQueue;

    @Value("${rabbitmq.queue.sms:pos.sms.queue}")
    private String smsQueue;

    @Value("${rabbitmq.queue.notification:pos.notification.queue}")
    private String notificationQueue;

    @Value("${rabbitmq.routing.email:pos.email.routing}")
    private String emailRoutingKey;

    @Value("${rabbitmq.routing.sms:pos.sms.routing}")
    private String smsRoutingKey;

    @Value("${rabbitmq.routing.notification:pos.notification.routing}")
    private String notificationRoutingKey;

    // Exchange
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // Queues
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(emailQueue).build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(smsQueue).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue).build();
    }

    // Bindings
    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange exchange) {
        return BindingBuilder.bind(emailQueue).to(exchange).with(emailRoutingKey);
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange exchange) {
        return BindingBuilder.bind(smsQueue).to(exchange).with(smsRoutingKey);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue).to(exchange).with(notificationRoutingKey);
    }

    // Message Converter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
