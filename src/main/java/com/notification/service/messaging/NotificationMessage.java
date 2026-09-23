package com.notification.service.messaging;

import com.notification.service.notification.Notification;
import java.util.UUID;

// Only the id travels through the queue: the database row is the single source of truth for the content.
public record NotificationMessage(UUID id) {

	public static NotificationMessage from(Notification notification) {
		return new NotificationMessage(notification.getId());
	}

}
