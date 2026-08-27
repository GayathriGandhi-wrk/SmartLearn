package com.student.performance.controller;

import com.student.performance.dto.AnalyticsDto;
import com.student.performance.dto.ApiResponse;
import com.student.performance.service.NotificationService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "User notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationController(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get notifications for the current student")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.NotificationDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.getNotifications(securityUtils.getCurrentStudentId())));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id, securityUtils.getCurrentStudentId());
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead(securityUtils.getCurrentStudentId());
        return ResponseEntity.ok(ApiResponse.ok("All marked as read", null));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.unreadCount(securityUtils.getCurrentStudentId())));
    }
}
