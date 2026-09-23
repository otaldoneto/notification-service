package com.notification.service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE = "notifications";
	public static final String EMAIL_QUEUE = "notifications.email";
	public static final String EMAIL_ROUTING_KEY = "notification.email";

	@Bean
	DirectExchange notificationsExchange() {
		return new DirectExchange(EXCHANGE);
	}

	@Bean
	Queue emailQueue() {
		return QueueBuilder.durable(EMAIL_QUEUE).build();
	}

	@Bean
	Binding emailBinding(Queue emailQueue, DirectExchange notificationsExchange) {
		return BindingBuilder.bind(emailQueue).to(notificationsExchange).with(EMAIL_ROUTING_KEY);
	}

	// Messages travel as JSON instead of Java serialization, so any language can read them.
	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

}
