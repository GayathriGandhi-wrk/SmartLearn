package com.student.performance.repository;

import com.student.performance.entity.ChatHistory;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findByStudentOrderByCreatedAtDesc(Student student);
    List<ChatHistory> findTop50ByStudentOrderByCreatedAtDesc(Student student);
}
