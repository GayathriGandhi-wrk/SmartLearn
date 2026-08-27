package com.student.performance.repository;

import com.student.performance.entity.LoginHistory;
import com.student.performance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    List<LoginHistory> findByUserOrderByLoginAtDesc(User user);
    List<LoginHistory> findTop10ByUserOrderByLoginAtDesc(User user);
}
