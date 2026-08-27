package com.student.performance.repository;

import com.student.performance.entity.Badge;
import com.student.performance.entity.Reward;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByStudentOrderByEarnedAtDesc(Student student);
    Optional<Reward> findByStudentAndBadge_BadgeId(Student student, Long badgeId);
    boolean existsByStudentAndBadge_BadgeId(Student student, Long badgeId);
}
