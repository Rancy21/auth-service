package com.larr.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String email, String token) {
        String verficationLink = frontendUrl + "/verify?token=" + token;
        String Subject = "Verfiy your email";
        String body = """
                Welcome! Please verify you email by clicking the link below:

                %s

                This link expires in 24 hours

                If you didn't create an account, you can safely ignore this email.
                """.formatted(verficationLink);

        sendEmail(email, Subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String email, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset your password";
        String body = """
                You requested to reset your password. Click the link below:
                %s
                This link expires in 1 hour.
                If you didn't request this, you can safely ignore this email.
                """.formatted(resetLink);
        sendEmail(email, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@authservice.com");

            mailSender.send(message);
            log.info("Email sent to {}", to);

        } catch (Exception e) {
            log.error("Fail to send email to :{}", to, e);
        }
    }

}