package com.notification.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.notification.service.TestcontainersConfiguration;
import com.notification.service.api.NotificationRequest;
import com.notification.service.config.RabbitConfig;
import com.notification.service.email.EmailSender;
import com.notification.service.notification.Notification;
import com.notification.service.notification.NotificationService;
import com.notification.service.notification.NotificationStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = { "spring.rabbitmq.listener.simple.retry.initial-interval=50ms",
		"spring.rabbitmq.listener.simple.retry.max-interval=200ms" })
@Import(TestcontainersConfiguration.class)
class NotificationRetryTest {

	private static final int MAX_ATTEMPTS = 4; // first attempt + 3 retries

	@Autowired
	NotificationService notificationService;

	@Autowired
	NotificationPublisher publisher;

	@Autowired
	RabbitTemplate rabbitTemplate;

	@MockitoBean
	EmailSender emailSender;

	@BeforeEach
	void emptyDeadLetterQueue() {
		while (rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE) != null) {
			// drain leftovers from previous tests
		}
	}

	@Test
	void transientFailureIsRetriedUntilItSucceeds() {
		doThrow(new MailSendException("SMTP down"))
			.doThrow(new MailSendException("SMTP down"))
			.doNothing()
			.when(emailSender).send(any());

		UUID id = create().getId();

		verify(emailSender, timeout(5_000).times(3)).send(any());
		awaitStatus(id, NotificationStatus.SENT);
		assertThat(rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 500)).isNull();
	}

	@Test
	void messageThatKeepsFailingEndsUpInTheDeadLetterQueue() {
		doThrow(new MailSendException("SMTP down")).when(emailSender).send(any());

		UUID id = create().getId();

		Message dead = rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 10_000);
		assertThat(dead).isNotNull();
		assertThat(new String(dead.getBody())).contains(id.toString());
		assertThat((String) dead.getMessageProperties().getHeader("x-exception-message")).contains("SMTP down");
		verify(emailSender, times(MAX_ATTEMPTS)).send(any());
		Notification failed = awaitStatus(id, NotificationStatus.FAILED);
		assertThat(failed.getLastError()).contains("SMTP down");
	}

	@Test
	void permanentFailureSkipsRetriesAndGoesStraightToTheDeadLetterQueue() {
		doThrow(new MailParseException("Invalid address")).when(emailSender).send(any());

		UUID id = create().getId();

		assertThat(rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 10_000)).isNotNull();
		verify(emailSender, times(1)).send(any());
		awaitStatus(id, NotificationStatus.FAILED);
	}

	@Test
	void malformedMessageGoesToTheDeadLetterQueueWithoutReachingTheSender() {
		MessageProperties properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send(RabbitConfig.EXCHANGE, RabbitConfig.EMAIL_ROUTING_KEY,
				new Message("{not json".getBytes(), properties));

		Message dead = rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 10_000);
		assertThat(dead).isNotNull();
		assertThat(new String(dead.getBody())).isEqualTo("{not json");
		verify(emailSender, never()).send(any());
	}

	@Test
	void unknownNotificationIsDeadLetteredWithoutRetries() {
		publisher.publish(new NotificationMessage(UUID.randomUUID()));

		Message dead = rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 10_000);
		assertThat(dead).isNotNull();
		assertThat((String) dead.getMessageProperties().getHeader("x-exception-message")).contains("not found");
		verify(emailSender, never()).send(any());
	}

	@Test
	void duplicateDeliveryDoesNotSendTheEmailTwice() {
		Notification notification = create();
		awaitStatus(notification.getId(), NotificationStatus.SENT);

		// Simulates the broker redelivering a message whose ack was lost.
		publisher.publish(NotificationMessage.from(notification));
		// A single consumer handles messages in order, so once this one is sent the duplicate was processed.
		UUID next = create().getId();
		awaitStatus(next, NotificationStatus.SENT);

		verify(emailSender, times(2)).send(any());
	}

	private Notification create() {
		return notificationService.create(new NotificationRequest("client@example.com", "Hi", "Hello"));
	}

	private Notification awaitStatus(UUID id, NotificationStatus status) {
		return await().atMost(Duration.ofSeconds(10))
			.until(() -> notificationService.find(id), notification -> notification.getStatus() == status);
	}

}
