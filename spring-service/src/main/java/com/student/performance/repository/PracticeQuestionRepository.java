package com.student.performance.repository;

import com.student.performance.entity.PracticeQuestion;
import com.student.performance.entity.Question;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {
    Optional<PracticeQuestion> findByStudentAndQuestion(Student student, Question question);
    List<PracticeQuestion> findByStudentAndBookmarkedTrue(Student student);
    List<PracticeQuestion> findByStudent(Student student);
    long countByStudent_StudentIdAndBookmarkedTrue(Long studentId);
    long countByStudent_StudentIdAndSolvedTrue(Long studentId);

    @Query("SELECT p.question.id FROM PracticeQuestion p WHERE p.student.id = :studentId")
    List<Long> findServedQuestionIds(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(p) FROM PracticeQuestion p WHERE p.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);
}
