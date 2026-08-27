package com.student.performance.repository;

import com.student.performance.entity.PracticeQuestion;
import com.student.performance.entity.Question;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeQuestionRepository extends JpaRepository<PracticeQuestion, Long> {
    Optional<PracticeQuestion> findByStudentAndQuestion(Student student, Question question);
    List<PracticeQuestion> findByStudentAndBookmarkedTrue(Student student);
    List<PracticeQuestion> findByStudent(Student student);
    long countByStudent_StudentIdAndBookmarkedTrue(Long studentId);
    long countByStudent_StudentIdAndSolvedTrue(Long studentId);
}
