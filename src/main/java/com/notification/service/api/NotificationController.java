package com.notification.service.api;

import com.notification.service.messaging.NotificationMessage;
import com.notification.service.messaging.NotificationPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationPublisher publisher;

	public NotificationController(NotificationPublisher publisher) {
		this.publisher = publisher;
	}

	// 202 Accepted: the request was queued, the e-mail is sent later in the background.
	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public NotificationResponse create(@Valid @RequestBody NotificationRequest request) {
		NotificationMessage message = NotificationMessage.from(request);
		publisher.publish(message);
		return new NotificationResponse(message.id(), "QUEUED");
	}

}
