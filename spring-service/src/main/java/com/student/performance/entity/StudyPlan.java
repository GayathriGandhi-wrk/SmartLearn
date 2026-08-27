package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "subject_name", length = 150)
    private String subjectName;

    @Column(name = "topic_name", length = 150)
    private String topicName;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 10)
    private PlanType planType = PlanType.DAILY;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_hours", nullable = false)
    private int totalHours = 4;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.ACTIVE;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean aiGenerated = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum PlanType { DAILY, WEEKLY, MONTHLY, EXAM }

    public enum Status { ACTIVE, COMPLETED, PAUSED }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
