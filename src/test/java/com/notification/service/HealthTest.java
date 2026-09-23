package com.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.containers.GenericContainer;

// The extra property gives this test its own Spring context (and its own containers), so stopping
// Mailpit here does not affect the other tests.
@SpringBootTest(properties = "test.context=health")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HealthTest {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	GenericContainer<?> mailpitContainer;

	// E-mails wait in the queue while the SMTP server is down, so the service must still report itself as healthy.
	@Test
	void serviceIsHealthyWhileTheMailServerIsDown() {
		mailpitContainer.stop();

		assertThat(mvc.get().uri("/actuator/health")).hasStatus(200).bodyJson().extractingPath("$.status").isEqualTo("UP");
	}

}
