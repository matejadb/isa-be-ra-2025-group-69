package com.project.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.url}")
    private String appUrl;

    public void sendActivationEmail(String to, String activationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Account Activation - Jutjubic");
        message.setText("Welcome to Jutjubic!\n\n" +
                "Click the link below to activate your account:\n" +
                appUrl + "/activate?token=" + activationToken + "\n\n" +
                "Link will expire in 24 hours.\n\n" +
                "If you did not sign up for this account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The Jutjubic Team");

        mailSender.send(message);
    }
}
