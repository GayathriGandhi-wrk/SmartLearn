package com.student.performance.repository;

import com.student.performance.entity.Question;
import com.student.performance.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByTopic(Topic topic);

    List<Question> findByTopic_Subject_SubjectId(Long subjectId);
    @Query("SELECT q FROM Question q WHERE q.difficulty = :difficulty AND q.active = true ORDER BY RAND()")
    List<Question> findRandomByDifficulty(@Param("difficulty") Question.Difficulty difficulty);

    @Query("SELECT q FROM Question q WHERE q.topic.subject.id = :subjectId AND q.difficulty = :difficulty AND q.active = true ORDER BY RAND()")
    List<Question> findRandomBySubjectAndDifficulty(@Param("subjectId") Long subjectId,
                                                     @Param("difficulty") Question.Difficulty difficulty);

    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId AND q.active = true ORDER BY RAND()")
    List<Question> findRandomByTopic(@Param("topicId") Long topicId);

    @Query("SELECT q FROM Question q WHERE q.topic.id = :topicId AND q.difficulty = :difficulty AND q.active = true ORDER BY RAND()")
    List<Question> findRandomByTopicAndDifficulty(@Param("topicId") Long topicId,
                                                   @Param("difficulty") Question.Difficulty difficulty);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.difficulty = :difficulty AND q.active = true")
    long countByDifficulty(@Param("difficulty") Question.Difficulty difficulty);

    @Query("SELECT COUNT(q) FROM Question q WHERE q.active = true")
    long countActive();
}
