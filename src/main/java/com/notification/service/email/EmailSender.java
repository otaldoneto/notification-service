package com.notification.service.email;

import com.notification.service.notification.Notification;
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

	public void send(Notification notification) {
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(from);
		mail.setTo(notification.getRecipient());
		mail.setSubject(notification.getSubject());
		mail.setText(notification.getBody());
		mailSender.send(mail);
	}

}
