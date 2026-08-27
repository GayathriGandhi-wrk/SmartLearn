package com.student.performance.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();
        java.lang.reflect.Field field = JwtService.class.getDeclaredField("secret");
        field.setAccessible(true);
        field.set(jwtService, "c2VjcmV0LWtleS1mb3Itc3R1ZGVudC1wZXJmb3JtYW5jZS1zeXN0ZW0tMzItYnl0ZXMtbG9uZw==");
        java.lang.reflect.Field expField = JwtService.class.getDeclaredField("expirationMs");
        expField.setAccessible(true);
        expField.set(jwtService, 3600000L);
        java.lang.reflect.Field remField = JwtService.class.getDeclaredField("rememberMeExpirationMs");
        remField.setAccessible(true);
        remField.set(jwtService, 604800000L);
        jwtService.init();
    }

    @Test
    void tokenGenerationAndValidation() {
        String token = jwtService.generateToken("student@test.com", 42L, "STUDENT", false);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("student@test.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("STUDENT");

        UserDetails user = new User("student@test.com", "pass", List.of());
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
