package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.PredictionDto;
import com.student.performance.service.RecommendationService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
@Tag(name = "Recommendations", description = "Personalized study recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final SecurityUtils securityUtils;

    public RecommendationController(RecommendationService recommendationService, SecurityUtils securityUtils) {
        this.recommendationService = recommendationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @Operation(summary = "Get recommendations for the current student")
    public ResponseEntity<ApiResponse<List<PredictionDto.RecommendationDto>>> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.getRecommendations(securityUtils.getCurrentStudentId(), unreadOnly)));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate fresh recommendations from current performance data")
    public ResponseEntity<ApiResponse<List<PredictionDto.RecommendationDto>>> generate() {
        return ResponseEntity.ok(ApiResponse.ok(
                recommendationService.generateRecommendations(securityUtils.getCurrentStudentId())));
    }

    @PostMapping("/{id}/viewed")
    @Operation(summary = "Mark a recommendation as viewed")
    public ResponseEntity<ApiResponse<Void>> markViewed(@PathVariable Long id) {
        recommendationService.markViewed(id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as viewed", null));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread recommendations")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.ok(recommendationService.unreadCount(securityUtils.getCurrentStudentId())));
    }
}
