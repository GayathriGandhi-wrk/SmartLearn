package com.student.performance.service.impl;

import com.student.performance.dto.StudentDto;
import com.student.performance.entity.Student;
import com.student.performance.entity.User;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.UserRepository;
import com.student.performance.service.FileStorageService;
import com.student.performance.service.StudentService;
import com.student.performance.util.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(StudentRepository studentRepository,
                              UserRepository userRepository,
                              SecurityUtils securityUtils,
                              FileStorageService fileStorageService,
                              PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public StudentDto.StudentProfileResponse getProfile() {
        Student student = securityUtils.getCurrentStudent();
        return StudentDto.StudentProfileResponse.from(student);
    }

    @Override
    @Transactional
    public StudentDto.StudentProfileResponse updateProfile(StudentDto.ProfileUpdateRequest request) {
        Student student = securityUtils.getCurrentStudent();
        User user = student.getUser();

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone());
        }
        if (request.address() != null) {
            student.setAddress(request.address());
        }
        if (request.dateOfBirth() != null) {
            student.setDateOfBirth(request.dateOfBirth());
        }
        if (request.gender() != null && !request.gender().isBlank()) {
            try {
                student.setGender(Student.Gender.valueOf(request.gender().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid gender value");
            }
        }
        if (request.department() != null && !request.department().isBlank()) {
            student.setDepartment(request.department());
        }
        if (request.semester() != null) {
            if (request.semester() < 1 || request.semester() > 8) {
                throw new BadRequestException("Semester must be between 1 and 8");
            }
            student.setSemester(request.semester());
        }
        if (request.cgpa() != null) {
            if (request.cgpa().compareTo(BigDecimal.ZERO) < 0 || request.cgpa().compareTo(new BigDecimal("10")) > 0) {
                throw new BadRequestException("CGPA must be between 0 and 10");
            }
            student.setCgpa(request.cgpa());
        }
        if (request.enrollmentYear() != null) {
            student.setEnrollmentYear(request.enrollmentYear());
        }
        userRepository.save(user);
        studentRepository.save(student);
        return StudentDto.StudentProfileResponse.from(student);
    }

    @Override
    @Transactional
    public StudentDto.StudentProfileResponse updateAcademic(StudentDto.AcademicDetailsRequest request) {
        Student student = securityUtils.getCurrentStudent();
        student.setDepartment(request.department());
        student.setSemester(request.semester());
        student.setBatch(request.batch());
        student.setCgpa(request.cgpa());
        student.setEnrollmentYear(request.enrollmentYear());
        studentRepository.save(student);
        return StudentDto.StudentProfileResponse.from(student);
    }

    @Override
    @Transactional
    public StudentDto.StudentProfileResponse uploadProfilePhoto(MultipartFile file) {
        Student student = securityUtils.getCurrentStudent();
        if (student.getProfilePhoto() != null) {
            fileStorageService.delete(student.getProfilePhoto());
        }
        String path = fileStorageService.store(file, "profiles");
        student.setProfilePhoto(path);
        studentRepository.save(student);
        return StudentDto.StudentProfileResponse.from(student);
    }

    @Override
    @Transactional
    public void deleteAccount() {
        Student student = securityUtils.getCurrentStudent();
        fileStorageService.delete(student.getProfilePhoto());
        User user = student.getUser();
        studentRepository.delete(student);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        User user = securityUtils.getCurrentUser();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
