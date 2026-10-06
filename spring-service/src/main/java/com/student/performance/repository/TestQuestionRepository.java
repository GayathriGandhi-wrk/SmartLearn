package com.student.performance.repository;

import com.student.performance.entity.Test;
import com.student.performance.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
    List<TestQuestion> findByTestOrderByOrderNoAsc(Test test);
    void deleteByTest(Test test);
    List<TestQuestion> findByTest_Student_StudentIdOrderByTest_TestIdDesc(Long studentId);

    /**
     * Topics this student has already finished a test on.
     *
     * <p>The test row itself carries no topic, so the link is made through the
     * questions the test served. Only COMPLETED tests count: a test the student
     * abandoned or that timed out has told us nothing about what they know, so
     * the topic stays on the "ready to test" list.
     */
    @Query("""
            SELECT DISTINCT q.topic.topicId
              FROM TestQuestion tq
              JOIN tq.question q
             WHERE tq.test.student.studentId = :studentId
               AND tq.test.status = 'COMPLETED'
               AND q.topic IS NOT NULL
            """)
    List<Long> findTestedTopicIds(@Param("studentId") Long studentId);
}
