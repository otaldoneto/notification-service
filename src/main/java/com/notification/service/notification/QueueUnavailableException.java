package com.notification.service.notification;

public class QueueUnavailableException extends RuntimeException {

	public QueueUnavailableException(Throwable cause) {
		super("Notification could not be queued", cause);
	}

}
