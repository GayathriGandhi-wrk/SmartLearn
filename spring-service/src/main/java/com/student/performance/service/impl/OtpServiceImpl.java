package com.student.performance.service.impl;

import com.student.performance.config.AppProperties;
import com.student.performance.entity.Otp;
import com.student.performance.exception.BadRequestException;
import com.student.performance.repository.OtpRepository;
import com.student.performance.service.OtpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final AppProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OtpServiceImpl(OtpRepository otpRepository, AppProperties properties) {
        this.otpRepository = otpRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public Otp generateAndStore(String email, String purpose) {
        otpRepository.findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .ifPresent(o -> {
                    o.setUsed(true);
                    otpRepository.save(o);
                });
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpCode(generateCode(properties.getOtp().getLength()));
        otp.setPurpose(purpose);
        otp.setUsed(false);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(properties.getOtp().getExpiryMinutes()));
        return otpRepository.save(otp);
    }

    @Override
    @Transactional
    public boolean validate(String email, String otpCode, String purpose) {
        Otp otp = otpRepository.findByEmailAndOtpCodeAndPurpose(email, otpCode, purpose)
                .orElseThrow(() -> new BadRequestException("Invalid OTP code"));
        if (otp.isUsed()) {
            throw new BadRequestException("OTP has already been used");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }
        otp.setUsed(true);
        otpRepository.save(otp);
        return true;
    }

    @Override
    @Transactional
    public void invalidate(String email, String purpose) {
        otpRepository.findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .ifPresent(o -> {
                    o.setUsed(true);
                    otpRepository.save(o);
                });
    }

    @Override
    public String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
