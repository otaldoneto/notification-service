package com.notification.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.notification.service.TestcontainersConfiguration;
import com.notification.service.config.RabbitConfig;
import com.notification.service.email.EmailSender;
import java.time.Duration;
import java.time.Instant;
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

		publisher.publish(message());

		verify(emailSender, timeout(5_000).times(3)).send(any());
		assertThat(rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 500)).isNull();
	}

	@Test
	void messageThatKeepsFailingEndsUpInTheDeadLetterQueue() {
		doThrow(new MailSendException("SMTP down")).when(emailSender).send(any());
		NotificationMessage message = message();

		publisher.publish(message);

		Message dead = rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 10_000);
		assertThat(dead).isNotNull();
		assertThat(new String(dead.getBody())).contains(message.id().toString());
		assertThat((String) dead.getMessageProperties().getHeader("x-exception-message")).contains("SMTP down");
		verify(emailSender, org.mockito.Mockito.times(MAX_ATTEMPTS)).send(any());
	}

	@Test
	void permanentFailureSkipsRetriesAndGoesStraightToTheDeadLetterQueue() {
		doThrow(new MailParseException("Invalid address")).when(emailSender).send(any());

		publisher.publish(message());

		assertThat(rabbitTemplate.receive(RabbitConfig.EMAIL_DEAD_LETTER_QUEUE, 10_000)).isNotNull();
		verify(emailSender, org.mockito.Mockito.times(1)).send(any());
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

	private static NotificationMessage message() {
		return new NotificationMessage(UUID.randomUUID(), "client@example.com", "Hi", "Hello", Instant.now());
	}

}
