package com.student.performance.repository;

import com.student.performance.entity.Test;
import com.student.performance.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
    List<TestQuestion> findByTestOrderByOrderNoAsc(Test test);
    void deleteByTest(Test test);
    List<TestQuestion> findByTest_Student_StudentIdOrderByTest_TestIdDesc(Long studentId);
}
