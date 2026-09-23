package com.notification.service.email;

import com.notification.service.messaging.NotificationMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

	private final JavaMailSender mailSender;
	private final String from;

	public EmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	public void send(NotificationMessage message) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(message.to());
		mail.setSubject(message.subject());
		mail.setText(message.body());
		mailSender.send(mail);
	}

}
