package com.student.performance.controller;

import com.student.performance.dto.AdminDto;
import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.SubjectDto;
import com.student.performance.service.AdminService;
import com.student.performance.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Administrator endpoints (ROLE_ADMIN only)")
public class AdminController {

    private final AdminService adminService;
    private final SubjectService subjectService;

    public AdminController(AdminService adminService, SubjectService subjectService) {
        this.adminService = adminService;
        this.subjectService = subjectService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard statistics")
    public ResponseEntity<ApiResponse<AdminDto.DashboardStats>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (optionally filtered by role)")
    public ResponseEntity<ApiResponse<List<AdminDto.UserResponse>>> users(
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listUsers(role)));
    }

    @PutMapping("/users/{userId}/toggle")
    @Operation(summary = "Activate / deactivate a user")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long userId) {
        adminService.toggleUserStatus(userId);
        return ResponseEntity.ok(ApiResponse.ok("User status updated", null));
    }

    @GetMapping("/students/{studentId}/overview")
    @Operation(summary = "Get a student's details, marks and performance analysis")
    public ResponseEntity<ApiResponse<AdminDto.StudentOverviewResponse>> studentOverview(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStudentOverview(studentId)));
    }

    @PostMapping("/questions/import")
    @Operation(summary = "Bulk import questions")
    public ResponseEntity<ApiResponse<Long>> importQuestions(@RequestBody AdminDto.QuestionImportRequest request) {
        long count = adminService.importQuestions(request.questions());
        return ResponseEntity.ok(ApiResponse.ok("Imported " + count + " questions", count));
    }

    @PostMapping("/subjects")
    @Operation(summary = "Create a new subject")
    public ResponseEntity<ApiResponse<SubjectDto.SubjectResponse>> createSubject(@RequestBody SubjectDto.SubjectRequest request) {
        SubjectDto.SubjectResponse created = subjectService.createSubject(request);
        return ResponseEntity.ok(ApiResponse.ok("Subject created", created));
    }

    @PostMapping("/subjects/{subjectId}/topics")
    @Operation(summary = "Add a topic to a subject")
    public ResponseEntity<ApiResponse<SubjectDto.TopicResponse>> createTopic(
            @PathVariable Long subjectId,
            @RequestBody SubjectDto.TopicRequest request) {
        SubjectDto.TopicResponse created = subjectService.createTopic(subjectId, request);
        return ResponseEntity.ok(ApiResponse.ok("Topic added", created));
    }
}
