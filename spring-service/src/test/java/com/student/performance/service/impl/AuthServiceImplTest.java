package com.student.performance.service.impl;

import com.student.performance.dto.AuthDto;
import com.student.performance.entity.Role;
import com.student.performance.entity.Student;
import com.student.performance.entity.User;
import com.student.performance.repository.*;
import com.student.performance.security.JwtService;
import com.student.performance.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private LoginHistoryRepository loginHistoryRepository;
    @Mock private JwtService jwtService;
    @Mock private com.student.performance.service.OtpService otpService;
    @Mock private com.student.performance.service.EmailService emailService;
    @Mock private com.student.performance.config.AppProperties properties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, roleRepository, studentRepository,
                loginHistoryRepository, new BCryptPasswordEncoder(), jwtService,
                otpService, emailService, properties);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "Dup User", "dup@test.com", "9876543210", "Test@1234", "CS", 8.0);
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(com.student.performance.exception.DuplicateResourceException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesUserAndStudent() {
        com.student.performance.config.AppProperties.Jwt jwtProps =
                new com.student.performance.config.AppProperties.Jwt();
        jwtProps.setExpirationMs(86400000);
        when(properties.getJwt()).thenReturn(jwtProps);

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findByRoleName("STUDENT")).thenReturn(Optional.of(role()));

        User saved = new User();
        saved.setUserId(1L);
        saved.setEmail("new@test.com");
        saved.setRole(role());
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(studentRepository.save(any(Student.class))).thenReturn(new Student());
        when(studentRepository.existsByStudentCode(anyString())).thenReturn(false);
        when(jwtService.generateToken(anyString(), anyLong(), anyString(), anyBoolean())).thenReturn("token");

        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest(
                "New User", "new@test.com", "9876543210", "Test@1234", "CS", 8.0);
        AuthDto.AuthResponse response = authService.register(request);
        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.token()).isEqualTo("token");
        verify(studentRepository, atLeastOnce()).save(any(Student.class));
    }

    private Role role() {
        Role r = new Role();
        r.setRoleId(2L);
        r.setRoleName("STUDENT");
        return r;
    }
}
