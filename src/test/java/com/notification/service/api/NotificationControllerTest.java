package com.notification.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notification.service.notification.Notification;
import com.notification.service.notification.NotificationNotFoundException;
import com.notification.service.notification.NotificationService;
import com.notification.service.notification.QueueUnavailableException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	NotificationService notificationService;

	@Test
	void validRequestIsQueued() {
		Notification notification = new Notification("client@example.com", "Hi", "Hello", Instant.now());
		when(notificationService.create(any())).thenReturn(notification);

		assertThat(post("""
				{"to":"client@example.com","subject":"Hi","body":"Hello"}
				""")).hasStatus(202)
			.bodyJson()
			.hasPathSatisfying("$.id", id -> id.assertThat().isEqualTo(notification.getId().toString()))
			.extractingPath("$.status")
			.isEqualTo("QUEUED");
	}

	@Test
	void invalidEmailIsRejected() {
		assertThat(post("""
				{"to":"not-an-email","subject":"Hi","body":"Hello"}
				""")).hasStatus(400);

		verify(notificationService, never()).create(any());
	}

	@Test
	void blankFieldsAreRejected() {
		assertThat(post("""
				{"to":"client@example.com","subject":"","body":" "}
				""")).hasStatus(400);

		verify(notificationService, never()).create(any());
	}

	@Test
	void brokerDownReturnsServiceUnavailable() {
		when(notificationService.create(any()))
			.thenThrow(new QueueUnavailableException(new AmqpConnectException(new RuntimeException("down"))));

		assertThat(post("""
				{"to":"client@example.com","subject":"Hi","body":"Hello"}
				""")).hasStatus(503);
	}

	@Test
	void notificationStatusCanBeQueried() {
		Notification notification = new Notification("client@example.com", "Hi", "Hello", Instant.now());
		notification.markSent(Instant.now());
		when(notificationService.find(notification.getId())).thenReturn(notification);

		assertThat(mvc.get().uri("/notifications/{id}", notification.getId())).hasStatus(200)
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("SENT");
	}

	@Test
	void unknownNotificationReturnsNotFound() {
		UUID id = UUID.randomUUID();
		when(notificationService.find(id)).thenThrow(new NotificationNotFoundException(id));

		assertThat(mvc.get().uri("/notifications/{id}", id)).hasStatus(404);
	}

	private MockMvcTester.MockMvcRequestBuilder post(String json) {
		return mvc.post().uri("/notifications").contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
