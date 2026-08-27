package com.student.performance.service.impl;

import com.student.performance.config.AppProperties;
import com.student.performance.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public EmailServiceImpl(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendOtpEmail(String to, String purpose, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Your OTP for " + purpose + " - Student Performance System");
            message.setText("""
                    Hello,

                    Your One-Time Password (OTP) is: %s

                    This OTP is valid for %d minutes.
                    If you did not request this, please ignore this email.

                    - AI Student Performance System
                    """.formatted(otpCode, properties.getOtp().getExpiryMinutes()));
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception ex) {
            log.warn("Failed to send OTP email to {}: {}", to, ex.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to AI Student Performance System!");
            message.setText("""
                    Dear %s,

                    Welcome aboard! Your account has been created successfully.
                    Start your personalized learning journey now.

                    - AI Student Performance System
                    """.formatted(fullName));
            mailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (Exception ex) {
            log.warn("Failed to send welcome email to {}: {}", to, ex.getMessage());
        }
    }
}
