package com.student.performance.repository;

import com.student.performance.entity.Analytics;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {
    List<Analytics> findByStudentOrderByCreatedAtAsc(Student student);
    List<Analytics> findByStudentAndEventDateBetween(Student student, LocalDate from, LocalDate to);

    @Query("SELECT a.eventType, COUNT(a) FROM Analytics a WHERE a.student.id = :studentId AND a.eventDate >= :from GROUP BY a.eventType")
    List<Object[]> countEventsByType(@Param("studentId") Long studentId, @Param("from") LocalDate from);
}
