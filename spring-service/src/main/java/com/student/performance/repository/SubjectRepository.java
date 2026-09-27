package com.student.performance.repository;

import com.student.performance.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findBySubjectCode(String subjectCode);
    List<Subject> findByDepartmentIgnoreCaseAndActiveTrue(String department);
    List<Subject> findByActiveTrue();
    boolean existsBySubjectCode(String subjectCode);

    @Query("SELECT DISTINCT s.department FROM Subject s "
            + "WHERE s.active = true AND s.department IS NOT NULL AND TRIM(s.department) <> '' "
            + "ORDER BY s.department")
    List<String> findDistinctDepartments();
}
