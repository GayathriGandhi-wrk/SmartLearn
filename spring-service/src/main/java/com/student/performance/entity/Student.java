package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "student_code", nullable = false, unique = true, length = 30)
    private String studentCode;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false)
    private int semester = 1;

    @Column(length = 30)
    private String batch;

    private BigDecimal cgpa;

    @Column(name = "enrollment_year")
    private Integer enrollmentYear;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private int level = 1;

    @Column(name = "xp_points", nullable = false)
    private int xpPoints = 0;

    @Column(name = "streak_days", nullable = false)
    private int streakDays = 0;

    @Column(name = "profile_photo", length = 255)
    private String profilePhoto;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
