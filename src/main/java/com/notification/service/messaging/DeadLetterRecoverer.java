package com.notification.service.messaging;

import com.notification.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;

// Runs once the retries are exhausted: records the failure in the database, then moves the message to the DLQ.
public class DeadLetterRecoverer implements MessageRecoverer {

	private static final Logger log = LoggerFactory.getLogger(DeadLetterRecoverer.class);

	private final MessageRecoverer deadLetterPublisher;
	private final MessageConverter messageConverter;
	private final NotificationService notificationService;

	public DeadLetterRecoverer(MessageRecoverer deadLetterPublisher, MessageConverter messageConverter,
			NotificationService notificationService) {
		this.deadLetterPublisher = deadLetterPublisher;
		this.messageConverter = messageConverter;
		this.notificationService = notificationService;
	}

	@Override
	public void recover(Message message, Throwable cause) {
		try {
			if (messageConverter.fromMessage(message) instanceof NotificationMessage(var id)) {
				notificationService.markFailed(id, rootCauseMessage(cause));
			}
		}
		catch (RuntimeException ex) {
			// A malformed message has no readable id: it still goes to the DLQ below.
			log.warn("Could not record the failure of a dead-lettered message: {}", ex.getMessage());
		}
		deadLetterPublisher.recover(message, cause);
	}

	private static String rootCauseMessage(Throwable failure) {
		Throwable root = failure;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		return root.getClass().getSimpleName() + ": " + root.getMessage();
	}

}
