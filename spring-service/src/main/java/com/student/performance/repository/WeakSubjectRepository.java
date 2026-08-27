package com.student.performance.repository;

import com.student.performance.entity.Student;
import com.student.performance.entity.WeakSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WeakSubjectRepository extends JpaRepository<WeakSubject, Long> {
    List<WeakSubject> findByStudentOrderByWeaknessScoreAsc(Student student);
    Optional<WeakSubject> findByStudentAndSubject_SubjectId(Student student, Long subjectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WeakSubject ws where ws.student = :student")
    void deleteByStudent(@Param("student") Student student);
}
