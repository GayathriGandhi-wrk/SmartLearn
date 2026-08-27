package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.AuthDto;
import com.student.performance.service.AuthService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration, login, OTP and password reset endpoints")
public class AuthController {

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new student")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request,
                                                                   HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String agent = http.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authService.login(request, ip, agent)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout the current user")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout(securityUtils.getCurrentUser().getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset OTP to email")
    public ResponseEntity<ApiResponse<AuthDto.OtpResponse>> forgotPassword(@Valid @RequestBody AuthDto.OtpRequest request) {
        AuthDto.OtpResponse otp = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP sent to your email", otp));
    }

    @PostMapping("/send-otp")
    @Operation(summary = "Send OTP for verification purpose")
    public ResponseEntity<ApiResponse<AuthDto.OtpResponse>> sendOtp(@Valid @RequestBody AuthDto.OtpRequest request) {
        AuthDto.OtpResponse otp = authService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP sent to your email", otp));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP code")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody AuthDto.VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.ok("OTP verified successfully", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using verified OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<AuthDto.MeResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(authService.me(securityUtils.getCurrentUser().getUserId())));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password for the current user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(securityUtils.getCurrentUser().getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
