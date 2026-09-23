package com.notification.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.notification.service.messaging.NotificationMessage;
import com.notification.service.messaging.NotificationPublisher;
import org.junit.jupiter.api.Test;
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
	NotificationPublisher publisher;

	@Test
	void validRequestIsQueued() {
		assertThat(post("""
				{"to":"client@example.com","subject":"Hi","body":"Hello"}
				""")).hasStatus(202).bodyJson().extractingPath("$.id").isNotNull();

		verify(publisher).publish(any(NotificationMessage.class));
	}

	@Test
	void invalidEmailIsRejected() {
		assertThat(post("""
				{"to":"not-an-email","subject":"Hi","body":"Hello"}
				""")).hasStatus(400);

		verify(publisher, never()).publish(any());
	}

	@Test
	void blankFieldsAreRejected() {
		assertThat(post("""
				{"to":"client@example.com","subject":"","body":" "}
				""")).hasStatus(400);

		verify(publisher, never()).publish(any());
	}

	private MockMvcTester.MockMvcRequestBuilder post(String json) {
		return mvc.post().uri("/notifications").contentType(MediaType.APPLICATION_JSON).content(json);
	}

}
