package com.notification.service.messaging;

import com.notification.service.config.RabbitConfig;
import com.notification.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

	private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

	private final NotificationService notificationService;

	public NotificationConsumer(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	// Spring acks the message only if this method returns normally; an exception means no ack.
	@RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
	public void onMessage(NotificationMessage message) {
		log.info("Notification {} received", message.id());
		notificationService.deliver(message.id());
	}

}
