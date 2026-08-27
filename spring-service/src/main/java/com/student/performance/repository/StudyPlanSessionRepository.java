package com.student.performance.repository;

import com.student.performance.entity.StudyPlan;
import com.student.performance.entity.StudyPlanSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudyPlanSessionRepository extends JpaRepository<StudyPlanSession, Long> {

    List<StudyPlanSession> findByPlanOrderBySessionDateAscStartTimeAsc(StudyPlan plan);

    List<StudyPlanSession> findByPlan_Student_StudentIdOrderBySessionDateAscStartTimeAsc(Long studentId);

    long countByPlanAndStatus(StudyPlan plan, StudyPlanSession.Status status);

    long countByPlan(StudyPlan plan);
}
