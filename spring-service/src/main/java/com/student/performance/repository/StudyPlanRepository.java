package com.student.performance.repository;

import com.student.performance.entity.Student;
import com.student.performance.entity.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    List<StudyPlan> findByStudentOrderByStartDateDesc(Student student);
    List<StudyPlan> findByStudentAndStatus(Student student, StudyPlan.Status status);
}
