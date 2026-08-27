package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.TestDto;
import com.student.performance.service.TestService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tests")
@Tag(name = "Adaptive Tests", description = "Start, submit and review adaptive tests")
public class TestController {

    private final TestService testService;
    private final SecurityUtils securityUtils;

    public TestController(TestService testService, SecurityUtils securityUtils) {
        this.testService = testService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a new adaptive test")
    public ResponseEntity<ApiResponse<TestDto.ActiveTestResponse>> start(
            @RequestBody(required = false) TestDto.StartTestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                testService.startTest(securityUtils.getCurrentStudentId(), request)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get the current active test")
    public ResponseEntity<ApiResponse<TestDto.ActiveTestResponse>> active() {
        return ResponseEntity.ok(ApiResponse.ok(
                testService.getActiveTest(securityUtils.getCurrentStudentId())));
    }

    @PostMapping("/{testId}/submit")
    @Operation(summary = "Submit a test with answers")
    public ResponseEntity<ApiResponse<TestDto.TestResultDto>> submit(
            @PathVariable Long testId,
            @RequestBody TestDto.SubmitTestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                testService.submitTest(testId, securityUtils.getCurrentStudentId(), request)));
    }

    @GetMapping("/{testId}/result")
    @Operation(summary = "Get detailed result of a test")
    public ResponseEntity<ApiResponse<TestDto.TestResultDto>> result(@PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.ok(
                testService.getResult(testId, securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/history")
    @Operation(summary = "Get test history")
    public ResponseEntity<ApiResponse<List<TestDto.TestListItem>>> history() {
        return ResponseEntity.ok(ApiResponse.ok(
                testService.getTestHistory(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/reviewed-questions")
    @Operation(summary = "Get all questions from completed tests with the student's answers")
    public ResponseEntity<ApiResponse<List<TestDto.ReviewedTestQuestion>>> reviewedQuestions() {
        return ResponseEntity.ok(ApiResponse.ok(
                testService.getReviewedTestQuestions(securityUtils.getCurrentStudentId())));
    }
}
