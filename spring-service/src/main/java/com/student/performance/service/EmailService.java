package com.student.performance.service;

public interface EmailService {

    void sendOtpEmail(String to, String purpose, String otpCode);

    void sendWelcomeEmail(String to, String fullName);
}
