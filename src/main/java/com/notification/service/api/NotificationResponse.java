package com.notification.service.api;

import com.notification.service.notification.Notification;
import com.notification.service.notification.NotificationStatus;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String to, String subject, NotificationStatus status, String lastError,
		Instant createdAt, Instant sentAt) {

	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(notification.getId(), notification.getRecipient(), notification.getSubject(),
				notification.getStatus(), notification.getLastError(), notification.getCreatedAt(),
				notification.getSentAt());
	}

}
