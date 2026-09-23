package com.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.RestClient;

// End to end: HTTP request -> RabbitMQ -> consumer -> SMTP, with real RabbitMQ and Mailpit containers.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificationFlowTest {

	@Autowired
	MockMvcTester mvc;

	@Value("${test.mailpit.api-url}")
	String mailpitApiUrl;

	@Test
	void queuedNotificationArrivesInTheInbox() {
		assertThat(mvc.post().uri("/notifications").contentType(MediaType.APPLICATION_JSON).content("""
				{"to":"client@example.com","subject":"Order finished","body":"Your order #42 is ready."}
				""")).hasStatus(202).bodyJson().extractingPath("$.status").isEqualTo("QUEUED");

		RestClient mailpit = RestClient.create(mailpitApiUrl);
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			String inbox = mailpit.get().uri("/api/v1/search?query=to:client@example.com").retrieve().body(String.class);
			assertThat(inbox).contains("\"Subject\":\"Order finished\"");
		});
	}

}
