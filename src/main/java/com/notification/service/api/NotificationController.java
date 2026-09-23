package com.notification.service.api;

import com.notification.service.notification.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	// 202 Accepted: the request was queued, the e-mail is sent later in the background.
	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public NotificationResponse create(@Valid @RequestBody NotificationRequest request) {
		return NotificationResponse.from(notificationService.create(request));
	}

	@GetMapping("/{id}")
	public NotificationResponse get(@PathVariable UUID id) {
		return NotificationResponse.from(notificationService.find(id));
	}

}
