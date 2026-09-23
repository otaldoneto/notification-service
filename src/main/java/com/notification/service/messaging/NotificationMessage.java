package com.notification.service.messaging;

import com.notification.service.api.NotificationRequest;
import java.time.Instant;
import java.util.UUID;

public record NotificationMessage(UUID id, String to, String subject, String body, Instant createdAt) {

	public static NotificationMessage from(NotificationRequest request) {
		return new NotificationMessage(UUID.randomUUID(), request.to(), request.subject(), request.body(),
				Instant.now());
	}

}
