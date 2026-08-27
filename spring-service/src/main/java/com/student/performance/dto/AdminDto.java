package com.student.performance.dto;

import java.util.List;
import java.util.Map;

public class AdminDto {

    public record UserResponse(
            Long userId,
            String email,
            String fullName,
            String phone,
            String role,
            boolean active,
            boolean verified,
            String profileImage,
            Long studentId
    ) {}

    public record DashboardStats(
            long totalStudents,
            long totalQuestions,
            long totalTestsTaken,
            long totalPredictions,
            double averageScore,
            long highRiskStudents,
            long totalUsers,
            long totalSubjects,
            long totalTopics,
            List<Map<String, Object>> recentTests,
            List<Map<String, Object>> subjectWisePerformance,
            Map<String, Long> riskDistribution
    ) {}

    public record StudentOverviewResponse(
            Long userId,
            Long studentId,
            String fullName,
            String email,
            String phone,
            String studentCode,
            String department,
            Integer semester,
            Double cgpa,
            Integer level,
            Integer xpPoints,
            Integer streakDays,
            Integer enrollmentYear,
            String batch,
            String address,
            String gender,
            String dateOfBirth,
            boolean active,
            boolean verified,
            Long totalTests,
            Double avgPercentage,
            Double bestPercentage,
            Double latestPercentage,
            List<Map<String, Object>> testHistory,
            List<Map<String, Object>> subjectPerformance,
            List<Map<String, Object>> topicPerformance
    ) {}

    public record QuestionImportRequest(
            List<QuestionImportItem> questions
    ) {}

    public record QuestionImportItem(
            Long topicId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctAnswer,
            String explanation,
            String difficulty,
            Double marks
    ) {}
}
