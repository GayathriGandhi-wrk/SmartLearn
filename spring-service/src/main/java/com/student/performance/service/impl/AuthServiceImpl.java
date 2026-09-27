package com.student.performance.service.impl;

import com.student.performance.config.AppProperties;
import com.student.performance.dto.AuthDto;
import com.student.performance.entity.LoginHistory;
import com.student.performance.entity.Otp;
import com.student.performance.entity.Role;
import com.student.performance.entity.Student;
import com.student.performance.entity.User;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.DuplicateResourceException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.LoginHistoryRepository;
import com.student.performance.repository.RoleRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.UserRepository;
import com.student.performance.security.JwtService;
import com.student.performance.service.AuthService;
import com.student.performance.service.EmailService;
import com.student.performance.service.OtpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String PURPOSE_REGISTER = "REGISTRATION";
    private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final AppProperties properties;

    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           StudentRepository studentRepository,
                           LoginHistoryRepository loginHistoryRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           OtpService otpService,
                           EmailService emailService,
                           AppProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentRepository = studentRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        Role studentRole = roleRepository.findByRoleName("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT role not seeded"));

        User user = new User();
        user.setRole(studentRole);
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setVerified(false);
        user = userRepository.save(user);

        Student student = new Student();
        student.setUser(user);
        student.setStudentCode(generateStudentCode(request.department()));
        student.setDepartment(request.department());
        student.setBatch(String.valueOf(LocalDateTime.now().getYear()));
        student.setCgpa(request.cgpa() == null ? null : BigDecimal.valueOf(request.cgpa()));
        student.setEnrollmentYear(LocalDateTime.now().getYear());
        student.setLevel(1);
        student.setXpPoints(0);
        student.setStreakDays(0);
        studentRepository.save(student);

        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        String token = jwtService.generateToken(user.getEmail(), user.getUserId(),
                user.getRole().getRoleName(), false);
        return new AuthDto.AuthResponse(token, "Bearer", properties.getJwt().getExpirationMs(),
                user.getUserId(), user.getEmail(), user.getFullName(), "STUDENT", student.getStudentId(),
                user.isVerified(), "Registration successful");
    }

    @Override
    @Transactional
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLogin(user, ipAddress, userAgent, LoginHistory.Status.FAILED);
            throw new BadRequestException("Invalid email or password");
        }
        if (!user.isActive()) {
            recordLogin(user, ipAddress, userAgent, LoginHistory.Status.FAILED);
            throw new BadRequestException("Account is disabled. Contact administrator.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        if (request.rememberMe()) {
            user.setRememberToken(UUID.randomUUID().toString());
        }
        userRepository.save(user);
        recordLogin(user, ipAddress, userAgent, LoginHistory.Status.SUCCESS);

        String token = jwtService.generateToken(user.getEmail(), user.getUserId(),
                user.getRole().getRoleName(), request.rememberMe());
        long expiresIn = request.rememberMe()
                ? properties.getJwt().getRememberMeExpirationMs()
                : properties.getJwt().getExpirationMs();

        Long studentId = null;
        if (user.getRole().getRoleName().equalsIgnoreCase("STUDENT")) {
            studentId = studentRepository.findByUser_UserId(user.getUserId())
                    .map(Student::getStudentId).orElse(null);
        }
        return new AuthDto.AuthResponse(token, "Bearer", expiresIn,
                user.getUserId(), user.getEmail(), user.getFullName(),
                user.getRole().getRoleName(), studentId, user.isVerified(), "Login successful");
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setRememberToken(null);
            userRepository.save(u);
        });
    }

    @Override
    public AuthDto.OtpResponse sendOtp(AuthDto.OtpRequest request) {
        String purpose = normalizePurpose(request.purpose());
        if (PURPOSE_PASSWORD_RESET.equals(purpose)) {
            userRepository.findByEmail(request.email().trim().toLowerCase())
                    .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));
        }
        var otp = otpService.generateAndStore(request.email().trim().toLowerCase(), purpose);
        emailService.sendOtpEmail(request.email().trim().toLowerCase(), purpose, otp.getOtpCode());
        return new AuthDto.OtpResponse(otp.getOtpCode(), otp.getExpiresAt(),
                properties.getOtp().getExpiryMinutes());
    }

    @Override
    public void verifyOtp(AuthDto.VerifyOtpRequest request) {
        String purpose = normalizePurpose(request.purpose());
        otpService.validate(request.email().trim().toLowerCase(), request.otpCode(), purpose);
        if (PURPOSE_REGISTER.equals(purpose)) {
            userRepository.findByEmail(request.email().trim().toLowerCase()).ifPresent(u -> {
                u.setVerified(true);
                userRepository.save(u);
            });
        }
    }

    @Override
    @Transactional
    public void resetPassword(AuthDto.ResetPasswordRequest request) {
        String purpose = PURPOSE_PASSWORD_RESET;
        otpService.validate(request.email().trim().toLowerCase(), request.otpCode(), purpose);
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for this email"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setVerified(true);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthDto.MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Optional<Student> studentOpt = user.getRole().getRoleName().equalsIgnoreCase("STUDENT")
                ? studentRepository.findByUser_UserId(userId)
                : Optional.empty();
        Student s = studentOpt.orElse(null);
        return new AuthDto.MeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getRoleName(),
                user.getPhone(),
                user.isVerified(),
                user.isActive(),
                s != null ? s.getStudentId() : null,
                s != null ? s.getStudentCode() : null,
                s != null ? s.getDepartment() : null,
                s != null ? s.getSemester() : null,
                s != null && s.getCgpa() != null ? s.getCgpa().doubleValue() : null,
                user.getProfileImage());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private void recordLogin(User user, String ip, String agent, LoginHistory.Status status) {
        LoginHistory h = new LoginHistory();
        h.setUser(user);
        h.setIpAddress(ip);
        h.setUserAgent(agent != null && agent.length() > 255 ? agent.substring(0, 255) : agent);
        h.setDevice(agent != null && agent.contains("Mobile") ? "Mobile" : "Desktop");
        h.setStatus(status);
        loginHistoryRepository.save(h);
    }

    private String generateStudentCode(String department) {
        String prefix = department != null && department.length() >= 3
                ? department.replaceAll("[^A-Za-z]", "").substring(0, 3).toUpperCase()
                : "STU";
        String year = String.valueOf(LocalDateTime.now().getYear());
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        String code = prefix + year + unique;
        while (studentRepository.existsByStudentCode(code)) {
            unique = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
            code = prefix + year + unique;
        }
        return code;
    }

    private String normalizePurpose(String purpose) {
        String p = purpose == null ? "" : purpose.trim().toUpperCase();
        return switch (p) {
            case "PASSWORD_RESET", "FORGOT_PASSWORD", "RESET_PASSWORD" -> PURPOSE_PASSWORD_RESET;
            case "REGISTRATION", "EMAIL_VERIFICATION", "VERIFY_EMAIL" -> PURPOSE_REGISTER;
            default -> p;
        };
    }
}
