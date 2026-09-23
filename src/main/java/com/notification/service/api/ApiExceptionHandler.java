package com.notification.service.api;

import com.notification.service.notification.NotificationNotFoundException;
import com.notification.service.notification.QueueUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(NotificationNotFoundException.class)
	ProblemDetail notFound(NotificationNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(QueueUnavailableException.class)
	ProblemDetail queueUnavailable(QueueUnavailableException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"Notification could not be queued, try again later");
	}

}
