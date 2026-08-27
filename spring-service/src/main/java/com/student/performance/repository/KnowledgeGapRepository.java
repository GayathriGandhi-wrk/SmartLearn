package com.student.performance.repository;

import com.student.performance.entity.KnowledgeGap;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KnowledgeGapRepository extends JpaRepository<KnowledgeGap, Long> {
    List<KnowledgeGap> findByStudentOrderByMasteryLevelAsc(Student student);
    Optional<KnowledgeGap> findByStudentAndTopic_TopicId(Student student, Long topicId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from KnowledgeGap kg where kg.student = :student")
    void deleteByStudent(@Param("student") Student student);
}
