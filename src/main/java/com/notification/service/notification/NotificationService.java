package com.notification.service.notification;

import com.notification.service.api.NotificationRequest;
import com.notification.service.email.EmailSender;
import com.notification.service.messaging.NotificationMessage;
import com.notification.service.messaging.NotificationPublisher;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

	private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

	private final NotificationRepository repository;
	private final NotificationPublisher publisher;
	private final EmailSender emailSender;
	private final Clock clock;

	public NotificationService(NotificationRepository repository, NotificationPublisher publisher,
			EmailSender emailSender, Clock clock) {
		this.repository = repository;
		this.publisher = publisher;
		this.emailSender = emailSender;
		this.clock = clock;
	}

	// The row is committed before publishing, so the consumer always finds it. If the broker is down,
	// the row is marked FAILED and the caller gets an error instead of a false "queued".
	public Notification create(NotificationRequest request) {
		Notification notification = repository
			.save(new Notification(request.to(), request.subject(), request.body(), clock.instant()));
		try {
			publisher.publish(NotificationMessage.from(notification));
		}
		catch (AmqpException ex) {
			notification.markFailed("Could not queue: " + ex.getMessage());
			repository.save(notification);
			throw new QueueUnavailableException(ex);
		}
		return notification;
	}

	public Notification find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
	}

	// RabbitMQ delivers at least once, so the same message can arrive twice. The status in the database
	// is the source of truth: an already sent notification is acknowledged without sending it again.
	public void deliver(UUID id) {
		Notification notification = find(id);
		if (notification.isSent()) {
			log.info("Notification {} was already sent, skipping duplicate delivery", id);
			return;
		}
		emailSender.send(notification);
		notification.markSent(clock.instant());
		repository.save(notification);
		log.info("Notification {} sent to {}", id, notification.getRecipient());
	}

	public void markFailed(UUID id, String error) {
		repository.findById(id).filter(notification -> !notification.isSent()).ifPresent(notification -> {
			notification.markFailed(error);
			repository.save(notification);
		});
	}

}
