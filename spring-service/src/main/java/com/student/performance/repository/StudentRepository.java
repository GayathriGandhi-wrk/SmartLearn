package com.student.performance.repository;

import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser_UserId(Long userId);
    Optional<Student> findByStudentCode(String studentCode);
    boolean existsByStudentCode(String studentCode);

    @Query("SELECT s FROM Student s ORDER BY s.xpPoints DESC")
    List<Student> findTopByOrderByXpPointsDesc();

    @Query("SELECT s FROM Student s WHERE s.department = :department ORDER BY s.cgpa DESC")
    List<Student> findByDepartment(String department);
}
