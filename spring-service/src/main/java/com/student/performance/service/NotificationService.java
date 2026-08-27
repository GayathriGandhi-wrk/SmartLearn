package com.student.performance.service;

import com.student.performance.dto.AnalyticsDto;
import com.student.performance.entity.Notification;
import com.student.performance.entity.Student;

import java.util.List;

public interface NotificationService {

    Notification notify(Student student, String title, String message, String type, String linkUrl);

    List<AnalyticsDto.NotificationDto> getNotifications(Long studentId);

    void markRead(Long notificationId, Long studentId);

    void markAllRead(Long studentId);

    long unreadCount(Long studentId);
}
