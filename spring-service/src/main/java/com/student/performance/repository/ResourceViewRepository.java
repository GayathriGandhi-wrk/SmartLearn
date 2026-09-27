package com.student.performance.repository;

import com.student.performance.entity.ResourceView;
import com.student.performance.entity.Student;
import com.student.performance.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ResourceViewRepository extends JpaRepository<ResourceView, Long> {

    Optional<ResourceView> findByResourceKey(String resourceKey);

    List<ResourceView> findByStudentAndTopicOrderByLastViewedAtDesc(Student student, Topic topic);

    List<ResourceView> findByStudentOrderByLastViewedAtDesc(Student student);

    long countByStudent_StudentId(Long studentId);

    @Modifying
    @Query("""
            UPDATE ResourceView r
               SET r.lastViewedAt = :seenAt,
                   r.viewCount = r.viewCount + 1
             WHERE r.resourceKey = :key
            """)
    int touch(@Param("key") String key, @Param("seenAt") LocalDateTime seenAt);
}
