package com.student.performance.util;

import com.student.performance.entity.Student;
import com.student.performance.entity.User;
import com.student.performance.exception.UnauthorizedException;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public SecurityUtils(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    public Student getCurrentStudent() {
        User user = getCurrentUser();
        return studentRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Student profile not found"));
    }

    public Long getCurrentStudentId() {
        return getCurrentStudent().getStudentId();
    }

    public boolean isAdmin() {
        return getCurrentUser().getRole().getRoleName().equalsIgnoreCase("ADMIN");
    }
}
