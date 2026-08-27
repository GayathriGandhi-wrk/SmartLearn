package com.student.performance.repository;

import com.student.performance.entity.Student;
import com.student.performance.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByStudentOrderByCreatedAtDesc(Student student);
    List<Test> findByStudentAndStatus(Student student, Test.Status status);
    Optional<Test> findFirstByStudentAndStatusOrderByCreatedAtDesc(Student student, Test.Status status);
    long countByStudent_StudentId(Long studentId);
    long countByStudent_StudentIdAndStatus(Long studentId, Test.Status status);
}
