package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_id")
    private Long rewardId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id")
    private Badge badge;

    @Column(name = "reward_type", nullable = false, length = 30)
    private String rewardType = "BADGE";

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "earned_at", updatable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    void onCreate() {
        earnedAt = LocalDateTime.now();
    }
}
