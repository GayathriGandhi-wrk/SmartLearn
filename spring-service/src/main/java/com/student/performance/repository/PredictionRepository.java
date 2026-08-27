package com.student.performance.repository;

import com.student.performance.entity.Prediction;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    List<Prediction> findByStudentOrderByCreatedAtDesc(Student student);
    List<Prediction> findByStudent_StudentIdOrderByCreatedAtDesc(Long studentId);
    Optional<Prediction> findFirstByStudentOrderByCreatedAtDesc(Student student);
}
