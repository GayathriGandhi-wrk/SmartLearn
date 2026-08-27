package com.student.performance.dto;

import com.student.performance.entity.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class StudentDto {

    public record StudentProfileResponse(
            Long userId,
            Long studentId,
            String studentCode,
            String fullName,
            String email,
            String phone,
            String department,
            int semester,
            String batch,
            BigDecimal cgpa,
            Integer enrollmentYear,
            LocalDate dateOfBirth,
            String gender,
            String address,
            int level,
            int xpPoints,
            int streakDays,
            String profilePhoto,
            boolean verified,
            Map<String, Object> academicSummary
    ) {
        public static StudentProfileResponse from(Student s) {
            return new StudentProfileResponse(
                    s.getUser().getUserId(),
                    s.getStudentId(),
                    s.getStudentCode(),
                    s.getUser().getFullName(),
                    s.getUser().getEmail(),
                    s.getUser().getPhone(),
                    s.getDepartment(),
                    s.getSemester(),
                    s.getBatch(),
                    s.getCgpa(),
                    s.getEnrollmentYear(),
                    s.getDateOfBirth(),
                    s.getGender() == null ? null : s.getGender().name(),
                    s.getAddress(),
                    s.getLevel(),
                    s.getXpPoints(),
                    s.getStreakDays(),
                    s.getProfilePhoto() != null ? s.getProfilePhoto() : s.getUser().getProfileImage(),
                    s.getUser().isVerified(),
                    null
            );
        }
    }

    public record ProfileUpdateRequest(
            String fullName,
            String phone,
            String address,
            LocalDate dateOfBirth,
            String gender,
            String department,
            Integer semester,
            BigDecimal cgpa,
            Integer enrollmentYear
    ) {}

    public record AcademicDetailsRequest(
            String department,
            int semester,
            String batch,
            BigDecimal cgpa,
            Integer enrollmentYear
    ) {}
}
