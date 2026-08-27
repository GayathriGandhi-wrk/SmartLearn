package com.student.performance.repository;

import com.student.performance.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectCode(String subjectCode);
    List<Subject> findByDepartmentIgnoreCaseAndActiveTrue(String department);
    List<Subject> findByActiveTrue();
    List<Subject> findBySemester(Integer semester);
    boolean existsBySubjectCode(String subjectCode);
}
