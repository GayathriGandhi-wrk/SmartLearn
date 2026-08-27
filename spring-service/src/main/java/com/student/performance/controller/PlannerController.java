package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.PlannerDto;
import com.student.performance.service.PlannerService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/study-plans")
@Tag(name = "Study Planner", description = "Intelligent time-based study schedules")
public class PlannerController {

    private final PlannerService plannerService;
    private final SecurityUtils securityUtils;

    public PlannerController(PlannerService plannerService, SecurityUtils securityUtils) {
        this.plannerService = plannerService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate an intelligent study plan")
    public ResponseEntity<ApiResponse<PlannerDto.StudyPlanSummaryDto>> generate(
            @RequestBody PlannerDto.StudyPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.generatePlan(securityUtils.getCurrentStudentId(), request)));
    }

    @GetMapping
    @Operation(summary = "List the current student's study plans")
    public ResponseEntity<ApiResponse<List<PlannerDto.StudyPlanSummaryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.getPlans(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single study plan (owner only)")
    public ResponseEntity<ApiResponse<PlannerDto.StudyPlanSummaryDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.getPlan(securityUtils.getCurrentStudentId(), id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Regenerate/update a study plan")
    public ResponseEntity<ApiResponse<PlannerDto.StudyPlanSummaryDto>> regenerate(
            @PathVariable Long id, @RequestBody PlannerDto.StudyPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.regeneratePlan(securityUtils.getCurrentStudentId(), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a study plan (owner only)")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        plannerService.deletePlan(securityUtils.getCurrentStudentId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Study plan deleted"));
    }

    @PutMapping("/sessions/{sessionId}/complete")
    @Operation(summary = "Mark a session as completed")
    public ResponseEntity<ApiResponse<PlannerDto.SessionDto>> completeSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.completeSession(securityUtils.getCurrentStudentId(), sessionId)));
    }

    @PutMapping("/sessions/{sessionId}/skip")
    @Operation(summary = "Mark a session as skipped")
    public ResponseEntity<ApiResponse<PlannerDto.SessionDto>> skipSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.skipSession(securityUtils.getCurrentStudentId(), sessionId)));
    }

    @GetMapping("/{planId}/report")
    @Operation(summary = "Get a study plan report (owner only)")
    public ResponseEntity<ApiResponse<PlannerDto.StudyPlanSummaryDto>> report(@PathVariable Long planId) {
        return ResponseEntity.ok(ApiResponse.ok(
                plannerService.getReport(securityUtils.getCurrentStudentId(), planId)));
    }
}
