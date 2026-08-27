package com.student.performance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    public record TrendPoint(
            LocalDate date,
            BigDecimal percentage
    ) {}

    public record SubjectRadarPoint(
            String subject,
            double accuracy
    ) {}

    public record DistributionPoint(
            String difficulty,
            long correct,
            long incorrect
    ) {}

    public record DailyStudyPoint(
            LocalDate date,
            int plannedMinutes,
            int completedMinutes,
            int skippedMinutes
    ) {}

    public record SubjectStudyHours(
            String subject,
            double hours,
            long completedSessions
    ) {}

    public record StudyPlanProgress(
            int activePlans,
            long totalSessions,
            long completedSessions,
            long skippedSessions,
            long pendingSessions,
            int completionPercent,
            double hoursStudied,
            List<DailyStudyPoint> dailyProgress,
            List<SubjectStudyHours> subjectHours
    ) {}

    public record AnalyticsResponse(
            List<TrendPoint> weeklyTrend,
            List<TrendPoint> monthlyTrend,
            List<SubjectRadarPoint> subjectAccuracy,
            List<DistributionPoint> difficultyDistribution,
            Map<String, Long> activitySummary,
            long totalTests,
            long totalQuestionsAnswered,
            BigDecimal averageScore,
            int currentStreak,
            int level,
            int xpPoints,
            StudyPlanProgress studyPlanProgress
    ) {}

    public record NotificationDto(
            Long notificationId,
            String title,
            String message,
            String type,
            boolean read,
            String linkUrl,
            LocalDateTime createdAt
    ) {}

    public record LeaderboardEntry(
            Long studentId,
            String fullName,
            String department,
            int semester,
            int xpPoints,
            int level,
            int streakDays,
            int rank
    ) {}

    public record GamificationResponse(
            int level,
            int xpPoints,
            int xpToNextLevel,
            int currentStreak,
            int maxStreak,
            List<BadgeDto> badges,
            List<LeaderboardEntry> leaderboard
    ) {}

    public record BadgeDto(
            Long badgeId,
            String badgeName,
            String badgeIcon,
            String badgeDescription,
            boolean earned
    ) {}
}
