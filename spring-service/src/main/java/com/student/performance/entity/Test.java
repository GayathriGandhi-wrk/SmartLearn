package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Long testId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "test_type", nullable = false, length = 30)
    private String testType = "ADAPTIVE";

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "total_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalMarks;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 15;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "incorrect_count")
    private Integer incorrectCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    @Column(name = "difficulty_level", length = 20)
    private String difficultyLevel;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status { IN_PROGRESS, COMPLETED, EXPIRED, CANCELLED }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
