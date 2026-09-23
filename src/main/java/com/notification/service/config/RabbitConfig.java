package com.notification.service.config;

import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;

@Configuration
public class RabbitConfig {

	public static final String EXCHANGE = "notifications";
	public static final String EMAIL_QUEUE = "notifications.email";
	public static final String EMAIL_ROUTING_KEY = "notification.email";

	public static final String DEAD_LETTER_EXCHANGE = "notifications.dlx";
	public static final String EMAIL_DEAD_LETTER_QUEUE = "notifications.email.dlq";

	// Failures that will never succeed on a retry: the message itself is broken.
	private static final List<Class<? extends Throwable>> PERMANENT_FAILURES = List.of(
			MessageConversionException.class, MailParseException.class, MailPreparationException.class);

	@Bean
	DirectExchange notificationsExchange() {
		return new DirectExchange(EXCHANGE);
	}

	// Anything the broker rejects from this queue is routed to the dead letter exchange instead of being lost.
	@Bean
	Queue emailQueue() {
		return QueueBuilder.durable(EMAIL_QUEUE)
			.deadLetterExchange(DEAD_LETTER_EXCHANGE)
			.deadLetterRoutingKey(EMAIL_ROUTING_KEY)
			.build();
	}

	@Bean
	Binding emailBinding(Queue emailQueue, DirectExchange notificationsExchange) {
		return BindingBuilder.bind(emailQueue).to(notificationsExchange).with(EMAIL_ROUTING_KEY);
	}

	@Bean
	DirectExchange deadLetterExchange() {
		return new DirectExchange(DEAD_LETTER_EXCHANGE);
	}

	@Bean
	Queue emailDeadLetterQueue() {
		return QueueBuilder.durable(EMAIL_DEAD_LETTER_QUEUE).build();
	}

	@Bean
	Binding emailDeadLetterBinding(Queue emailDeadLetterQueue, DirectExchange deadLetterExchange) {
		return BindingBuilder.bind(emailDeadLetterQueue).to(deadLetterExchange).with(EMAIL_ROUTING_KEY);
	}

	// Messages travel as JSON instead of Java serialization, so any language can read them.
	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	// After the last retry, the message is republished to the DLQ with the failure reason in its headers
	// (x-exception-message, x-exception-stacktrace), so it can be inspected and replayed later.
	@Bean
	MessageRecoverer deadLetterRecoverer(RabbitTemplate rabbitTemplate) {
		return new RepublishMessageRecoverer(rabbitTemplate, DEAD_LETTER_EXCHANGE, EMAIL_ROUTING_KEY);
	}

	@Bean
	RabbitListenerRetrySettingsCustomizer skipRetryOnPermanentFailures() {
		return settings -> settings.setExceptionPredicate(RabbitConfig::isTransient);
	}

	static boolean isTransient(Throwable failure) {
		for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
			for (Class<? extends Throwable> permanent : PERMANENT_FAILURES) {
				if (permanent.isInstance(cause)) {
					return false;
				}
			}
		}
		return true;
	}

}
