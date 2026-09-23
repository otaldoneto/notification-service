package com.notification.service.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
		@NotBlank @Email String to,
		@NotBlank @Size(max = 200) String subject,
		@NotBlank @Size(max = 10_000) String body) {
}
