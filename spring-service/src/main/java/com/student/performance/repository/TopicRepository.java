package com.student.performance.repository;

import com.student.performance.entity.Subject;
import com.student.performance.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySubject(Subject subject);
    List<Topic> findBySubjectSubjectId(Long subjectId);
    List<Topic> findByDifficultyLevel(String difficultyLevel);
}
