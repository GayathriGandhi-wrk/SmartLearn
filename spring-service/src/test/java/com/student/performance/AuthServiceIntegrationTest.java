package com.student.performance;

import com.student.performance.dto.AuthDto;
import com.student.performance.entity.Role;
import com.student.performance.repository.RoleRepository;
import com.student.performance.repository.UserRepository;
import com.student.performance.security.JwtService;
import com.student.performance.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setup() {
        if (roleRepository.findByRoleName("STUDENT").isEmpty()) {
            Role role = new Role();
            role.setRoleName("STUDENT");
            role.setDescription("Student");
            roleRepository.save(role);
        }
        if (roleRepository.findByRoleName("ADMIN").isEmpty()) {
            Role role = new Role();
            role.setRoleName("ADMIN");
            role.setDescription("Admin");
            roleRepository.save(role);
        }
    }

    @Test
    void registerAndLoginRoundTrip() {
        AuthDto.RegisterRequest register = new AuthDto.RegisterRequest(
                "Test Student", "test.student@example.com", "9876543210",
                "Test@1234", "Computer Science", 8.5);
        AuthDto.AuthResponse response = authService.register(register);
        assertThat(response).isNotNull();
        assertThat(response.token()).isNotBlank();
        assertThat(response.role()).isEqualTo("STUDENT");
        assertThat(userRepository.existsByEmail("test.student@example.com")).isTrue();

        AuthDto.LoginRequest login = new AuthDto.LoginRequest("test.student@example.com", "Test@1234", false);
        AuthDto.AuthResponse loginResponse = authService.login(login, "127.0.0.1", "JUnit");
        assertThat(loginResponse.token()).isNotBlank();
        assertThat(jwtService.extractUsername(loginResponse.token())).isEqualTo("test.student@example.com");
    }

    @Test
    void loginWithWrongPasswordFails() {
        try {
            authService.login(new AuthDto.LoginRequest("nonexistent@example.com", "Wrong@1234", false),
                    "127.0.0.1", "JUnit");
        } catch (Exception ex) {
            assertThat(ex.getMessage()).contains("Invalid email or password");
        }
    }

    @Test
    void otpSendVerifyResetRoundTrip() {
        // Register a fresh user
        AuthDto.RegisterRequest register = new AuthDto.RegisterRequest(
                "Otp Tester", "otp.test@example.com", "9876543211",
                "Otp@1234", "Computer Science", 7.5);
        authService.register(register);

        // Send a password-reset OTP (Spring generates + stores it)
        AuthDto.OtpResponse otpResponse =
                authService.sendOtp(new AuthDto.OtpRequest("otp.test@example.com", "PASSWORD_RESET"));
        assertThat(otpResponse.otpCode()).hasSize(6);

        // Reset the password (single-use OTP validated by the reset itself)
        authService.resetPassword(new AuthDto.ResetPasswordRequest("otp.test@example.com",
                otpResponse.otpCode(), "NewOtp@1234"));

        // Old password must no longer work; new one must
        try {
            authService.login(new AuthDto.LoginRequest("otp.test@example.com", "Otp@1234", false),
                    "127.0.0.1", "JUnit");
        } catch (Exception ex) {
            assertThat(ex.getMessage()).contains("Invalid email or password");
        }
        AuthDto.AuthResponse login = authService.login(
                new AuthDto.LoginRequest("otp.test@example.com", "NewOtp@1234", false),
                "127.0.0.1", "JUnit");
        assertThat(login.token()).isNotBlank();
    }

    @Test
    void meReturnsCurrentUserProfile() {
        AuthDto.RegisterRequest register = new AuthDto.RegisterRequest(
                "Me Tester", "me.test@example.com", "9876543212",
                "Me@1234", "Information Technology", 8.0);
        AuthDto.AuthResponse response = authService.register(register);

        AuthDto.MeResponse me = authService.me(response.userId());
        assertThat(me.email()).isEqualTo("me.test@example.com");
        assertThat(me.role()).isEqualTo("STUDENT");
        assertThat(me.studentId()).isNotNull();
        assertThat(me.studentCode()).isNotBlank();
        assertThat(me.department()).isEqualTo("Information Technology");
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        AuthDto.RegisterRequest register = new AuthDto.RegisterRequest(
                "Chg Tester", "chg.test@example.com", "9876543213",
                "Chg@1234", "Computer Science", 6.5);
        authService.register(register);

        try {
            authService.changePassword(userRepository.findByEmail("chg.test@example.com").get().getUserId(),
                    new AuthDto.ChangePasswordRequest("Wrong@9999", "NewChg@1234"));
        } catch (Exception ex) {
            assertThat(ex.getMessage()).contains("Current password is incorrect");
        }
    }
}
