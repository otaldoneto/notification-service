package com.notification.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notification.service.api.NotificationRequest;
import com.notification.service.email.EmailSender;
import com.notification.service.messaging.NotificationPublisher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;

class NotificationServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

	private final NotificationRepository repository = mock(NotificationRepository.class);
	private final NotificationPublisher publisher = mock(NotificationPublisher.class);
	private final EmailSender emailSender = mock(EmailSender.class);
	private final NotificationService service = new NotificationService(repository, publisher, emailSender,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void saveReturnsTheEntity() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createStoresTheNotificationAsQueuedAndPublishesIt() {
		Notification notification = service.create(new NotificationRequest("client@example.com", "Hi", "Hello"));

		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.QUEUED);
		assertThat(notification.getCreatedAt()).isEqualTo(NOW);
		verify(publisher).publish(any());
	}

	@Test
	void createMarksTheNotificationFailedWhenTheBrokerIsDown() {
		doThrow(new AmqpConnectException(new RuntimeException("connection refused"))).when(publisher)
			.publish(any());

		assertThatThrownBy(() -> service.create(new NotificationRequest("client@example.com", "Hi", "Hello")))
			.isInstanceOf(QueueUnavailableException.class);

		ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
		verify(repository, atLeastOnce()).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
		assertThat(saved.getValue().getLastError()).contains("Could not queue");
	}

	@Test
	void deliverSendsTheEmailAndMarksItSent() {
		Notification notification = new Notification("client@example.com", "Hi", "Hello", NOW);
		when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

		service.deliver(notification.getId());

		verify(emailSender).send(notification);
		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
		assertThat(notification.getSentAt()).isEqualTo(NOW);
	}

	@Test
	void deliverSkipsNotificationsThatWereAlreadySent() {
		Notification notification = new Notification("client@example.com", "Hi", "Hello", NOW);
		notification.markSent(NOW);
		when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

		service.deliver(notification.getId());

		verify(emailSender, never()).send(any());
	}

	@Test
	void markFailedNeverOverwritesASentNotification() {
		Notification notification = new Notification("client@example.com", "Hi", "Hello", NOW);
		notification.markSent(NOW);
		when(repository.findById(notification.getId())).thenReturn(Optional.of(notification));

		service.markFailed(notification.getId(), "late failure");

		assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
	}

	@Test
	void longErrorsAreTruncatedToFitTheColumn() {
		Notification notification = new Notification("client@example.com", "Hi", "Hello", NOW);

		notification.markFailed("x".repeat(5_000));

		assertThat(notification.getLastError()).hasSize(1000);
	}

}
