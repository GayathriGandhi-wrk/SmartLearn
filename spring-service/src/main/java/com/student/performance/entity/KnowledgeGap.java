package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_gap")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gap_id")
    private Long gapId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "mastery_level", nullable = false, precision = 5, scale = 2)
    private BigDecimal masteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "gap_level", nullable = false, length = 10)
    private GapLevel gapLevel = GapLevel.MEDIUM;

    @Column(name = "attempted_questions", nullable = false)
    private int attemptedQuestions = 0;

    @Column(name = "correct_questions", nullable = false)
    private int correctQuestions = 0;

    @Column(name = "recommended_hours")
    private Integer recommendedHours;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum GapLevel { LOW, MEDIUM, HIGH, CRITICAL }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
