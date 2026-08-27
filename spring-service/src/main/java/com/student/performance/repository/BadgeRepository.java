package com.student.performance.repository;

import com.student.performance.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    java.util.List<Badge> findByThresholdLessThanEqualOrderByThresholdAsc(int threshold);
}
