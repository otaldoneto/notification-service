package com.notification.service.messaging;

import com.notification.service.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {

	private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public NotificationPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publish(NotificationMessage message) {
		rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.EMAIL_ROUTING_KEY, message);
		log.info("Notification {} published", message.id());
	}

}
