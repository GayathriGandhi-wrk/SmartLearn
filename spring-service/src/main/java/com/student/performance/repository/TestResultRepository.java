package com.student.performance.repository;

import com.student.performance.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByStudent_StudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT tr FROM TestResult tr WHERE tr.student.id = :studentId AND tr.createdAt >= :from ORDER BY tr.createdAt ASC")
    List<TestResult> findByStudentSince(@Param("studentId") Long studentId, @Param("from") LocalDateTime from);

    @Query("SELECT AVG(tr.percentage) FROM TestResult tr WHERE tr.student.id = :studentId")
    Double averagePercentage(@Param("studentId") Long studentId);
}
