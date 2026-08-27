package com.student.performance.service;

import com.student.performance.dto.AnalyticsDto;

import java.util.List;

public interface GamificationService {

    AnalyticsDto.GamificationResponse getGamification(Long studentId);

    List<AnalyticsDto.LeaderboardEntry> getLeaderboard(int limit);
}
