package com.student.performance.controller;

import com.student.performance.dto.AnalyticsDto;
import com.student.performance.dto.ApiResponse;
import com.student.performance.service.AnalyticsService;
import com.student.performance.service.GamificationService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics & Gamification", description = "Progress charts, leaderboard, badges and achievements")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final GamificationService gamificationService;
    private final SecurityUtils securityUtils;

    public AnalyticsController(AnalyticsService analyticsService,
                               GamificationService gamificationService,
                               SecurityUtils securityUtils) {
        this.analyticsService = analyticsService;
        this.gamificationService = gamificationService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview (trends, radar, distribution)")
    public ResponseEntity<ApiResponse<AnalyticsDto.AnalyticsResponse>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(
                analyticsService.getAnalytics(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/gamification")
    @Operation(summary = "Get gamification state (level, XP, badges, leaderboard)")
    public ResponseEntity<ApiResponse<AnalyticsDto.GamificationResponse>> gamification() {
        return ResponseEntity.ok(ApiResponse.ok(
                gamificationService.getGamification(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get top students by XP")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.LeaderboardEntry>>> leaderboard(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(gamificationService.getLeaderboard(limit)));
    }
}
