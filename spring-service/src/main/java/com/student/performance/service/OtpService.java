package com.student.performance.service;

import com.student.performance.entity.Otp;

public interface OtpService {

    Otp generateAndStore(String email, String purpose);

    boolean validate(String email, String otpCode, String purpose);

    void invalidate(String email, String purpose);

    String generateCode(int length);
}
