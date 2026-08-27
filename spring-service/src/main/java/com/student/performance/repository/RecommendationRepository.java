package com.student.performance.repository;

import com.student.performance.entity.Recommendation;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByStudentOrderByPriorityAscCreatedAtDesc(Student student);
    List<Recommendation> findByStudentAndTypeOrderByPriorityAsc(Student student, String type);
    List<Recommendation> findByStudentAndViewedFalseOrderByPriorityAsc(Student student);
    long countByStudent_StudentIdAndViewedFalse(Long studentId);
}
