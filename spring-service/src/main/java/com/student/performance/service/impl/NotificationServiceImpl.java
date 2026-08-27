package com.student.performance.service.impl;

import com.student.performance.dto.AnalyticsDto;
import com.student.performance.entity.Notification;
import com.student.performance.entity.Student;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.NotificationRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   StudentRepository studentRepository) {
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public Notification notify(Student student, String title, String message, String type, String linkUrl) {
        Notification n = new Notification();
        n.setStudent(student);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setRead(false);
        n.setLinkUrl(linkUrl);
        return notificationRepository.save(n);
    }

    @Override
    public List<AnalyticsDto.NotificationDto> getNotifications(Long studentId) {
        Student student = new Student();
        student.setStudentId(studentId);
        return notificationRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .map(n -> new AnalyticsDto.NotificationDto(
                        n.getNotificationId(), n.getTitle(), n.getMessage(), n.getType(),
                        n.isRead(), n.getLinkUrl(), n.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long studentId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Override
    @Transactional
    public void markAllRead(Long studentId) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return;
        notificationRepository.findByStudentAndReadFalseOrderByCreatedAtDesc(student).forEach(n -> n.setRead(true));
    }

    @Override
    public long unreadCount(Long studentId) {
        return notificationRepository.countByStudent_StudentIdAndReadFalse(studentId);
    }
}
