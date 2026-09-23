package com.notification.service.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

	private static final int MAX_ERROR_LENGTH = 1000;

	@Id
	private UUID id;

	@Column(nullable = false)
	private String recipient;

	@Column(nullable = false)
	private String subject;

	@Column(nullable = false)
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationStatus status;

	private String lastError;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant sentAt;

	protected Notification() {
		// required by JPA
	}

	public Notification(String recipient, String subject, String body, Instant createdAt) {
		this.id = UUID.randomUUID();
		this.recipient = recipient;
		this.subject = subject;
		this.body = body;
		this.status = NotificationStatus.QUEUED;
		this.createdAt = createdAt;
	}

	public void markSent(Instant sentAt) {
		this.status = NotificationStatus.SENT;
		this.sentAt = sentAt;
		this.lastError = null;
	}

	public void markFailed(String error) {
		this.status = NotificationStatus.FAILED;
		this.lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error
				: error.substring(0, MAX_ERROR_LENGTH);
	}

	public boolean isSent() {
		return status == NotificationStatus.SENT;
	}

	public UUID getId() {
		return id;
	}

	public String getRecipient() {
		return recipient;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public NotificationStatus getStatus() {
		return status;
	}

	public String getLastError() {
		return lastError;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getSentAt() {
		return sentAt;
	}

}
