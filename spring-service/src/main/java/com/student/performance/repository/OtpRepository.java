package com.student.performance.repository;

import com.student.performance.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, String purpose);
    Optional<Otp> findByEmailAndOtpCodeAndPurpose(String email, String otpCode, String purpose);
    void deleteByExpiresAtBefore(LocalDateTime before);
}
