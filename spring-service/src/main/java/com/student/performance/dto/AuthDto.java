package com.student.performance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class AuthDto {

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 3, max = 120, message = "Full name must be 3-120 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Invalid phone number")
            String phone,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 60, message = "Password must be 8-60 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                    message = "Password must contain uppercase, lowercase, digit and special character")
            String password,

            @NotBlank(message = "Department is required")
            String department,

            @DecimalMin(value = "0.0", message = "CGPA cannot be negative")
            @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10.0")
            Double cgpa
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password,

            boolean rememberMe
    ) {}

    public record OtpRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Purpose is required")
            String purpose
    ) {}

    public record VerifyOtpRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "OTP is required")
            String otpCode,

            @NotBlank(message = "Purpose is required")
            String purpose
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "OTP is required")
            String otpCode,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 60, message = "Password must be 8-60 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                    message = "Password must contain uppercase, lowercase, digit and special character")
            String newPassword
    ) {}

    public record OtpResponse(
            String otpCode,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime expiresAt,
            long expiresInMinutes
    ) {}

    public record MeResponse(
            Long userId,
            String email,
            String fullName,
            String role,
            String phone,
            boolean verified,
            boolean active,
            Long studentId,
            String studentCode,
            String department,
            Integer semester,
            Double cgpa,
            String profileImage
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required")
            String oldPassword,

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 60, message = "Password must be 8-60 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                    message = "Password must contain uppercase, lowercase, digit and special character")
            String newPassword
    ) {}

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresIn,
            Long userId,
            String email,
            String fullName,
            String role,
            Long studentId,
            boolean verified,
            String message
    ) {}
}
