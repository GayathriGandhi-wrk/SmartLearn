package com.student.performance.repository;

import com.student.performance.entity.Notification;
import com.student.performance.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudentOrderByCreatedAtDesc(Student student);
    List<Notification> findByStudentAndReadFalseOrderByCreatedAtDesc(Student student);
    long countByStudent_StudentIdAndReadFalse(Long studentId);
}
