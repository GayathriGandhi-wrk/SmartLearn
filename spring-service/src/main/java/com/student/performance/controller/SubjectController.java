package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.SubjectDto;
import com.student.performance.service.SubjectService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
@Tag(name = "Subjects", description = "Subject listing, topics and performance")
public class SubjectController {

    private final SubjectService subjectService;
    private final SecurityUtils securityUtils;

    public SubjectController(SubjectService subjectService, SecurityUtils securityUtils) {
        this.subjectService = subjectService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "List all subjects (optionally by department)")
    public ResponseEntity<ApiResponse<List<SubjectDto.SubjectResponse>>> listSubjects(
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.listSubjects(department)));
    }

    @GetMapping("/departments")
    @Operation(summary = "List the departments that have subjects")
    public ResponseEntity<ApiResponse<List<String>>> listDepartments() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.listDepartments()));
    }

    @GetMapping("/{subjectId}/topics")
    @Operation(summary = "List topics for a subject")
    public ResponseEntity<ApiResponse<List<SubjectDto.TopicResponse>>> listTopics(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.listTopics(subjectId)));
    }

    @GetMapping("/performance")
    @Operation(summary = "Get subject-wise performance for the current student")
    public ResponseEntity<ApiResponse<SubjectDto.PerformanceResponse>> performance() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getPerformance(securityUtils.getCurrentStudentId())));
    }

    @PostMapping("/marks")
    @Operation(summary = "Save marks, attendance, assignments and internal marks for subjects")
    public ResponseEntity<ApiResponse<Void>> saveMarks(@RequestBody List<SubjectDto.SubjectMarksRequest> requests) {
        subjectService.saveSubjectMarks(requests, securityUtils.getCurrentStudentId());
        return ResponseEntity.ok(ApiResponse.ok("Marks saved successfully", null));
    }
}
