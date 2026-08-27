package com.student.performance.repository;

import com.student.performance.entity.QuestionAttempt;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {

    List<QuestionAttempt> findByStudent(Student student);

    long countByStudent_StudentId(Long studentId);

    @Query("SELECT COUNT(qa) FROM QuestionAttempt qa WHERE qa.student.id = :studentId AND qa.correct = true")
    long countCorrect(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(qa) FROM QuestionAttempt qa WHERE qa.student.id = :studentId AND qa.correct = false")
    long countIncorrect(@Param("studentId") Long studentId);

    @Query("SELECT qa FROM QuestionAttempt qa WHERE qa.student.id = :studentId AND qa.attemptedAt >= :from")
    List<QuestionAttempt> findRecentByStudent(@Param("studentId") Long studentId, @Param("from") LocalDateTime from);

    @Query("SELECT qa FROM QuestionAttempt qa WHERE qa.student.id = :studentId AND qa.question.topic.id = :topicId")
    List<QuestionAttempt> findByStudentAndTopic(@Param("studentId") Long studentId, @Param("topicId") Long topicId);

    @Query("SELECT qa FROM QuestionAttempt qa WHERE qa.student.id = :studentId AND qa.question.topic.subject.id = :subjectId")
    List<QuestionAttempt> findByStudentAndSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);
}
