package com.student.performance.service;

import com.student.performance.dto.AuthDto;

public interface AuthService {

    AuthDto.AuthResponse register(AuthDto.RegisterRequest request);

    AuthDto.AuthResponse login(AuthDto.LoginRequest request, String ipAddress, String userAgent);

    void logout(Long userId);

    AuthDto.OtpResponse sendOtp(AuthDto.OtpRequest request);

    void verifyOtp(AuthDto.VerifyOtpRequest request);

    void resetPassword(AuthDto.ResetPasswordRequest request);

    AuthDto.MeResponse me(Long userId);

    void changePassword(Long userId, AuthDto.ChangePasswordRequest request);
}
