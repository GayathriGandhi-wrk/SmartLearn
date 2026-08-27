package com.student.performance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PredictionDto {

    public record PredictionRequest(
            Map<String, Object> features
    ) {}

    public record PredictionResponse(
            Long predictionId,
            String modelName,
            String predictedGrade,
            BigDecimal predictedScore,
            String riskLevel,
            BigDecimal confidenceScore,
            Map<String, Double> featureImportance,
            String explanation,
            List<WeakSubjectDto> weakSubjects,
            List<KnowledgeGapDto> knowledgeGaps,
            LocalDateTime createdAt
    ) {}

    public record PerformanceFactorDto(
            String factor,
            String value,
            String status
    ) {}

    public record PerformanceFactorsResponse(
            List<PerformanceFactorDto> factors,
            String summary
    ) {}

    public record TopicScoreDto(
            Long topicId,
            String topicName,
            String subjectName,
            BigDecimal score,
            int attemptedQuestions,
            int correctQuestions,
            String category
    ) {}

    public record TopicPredictionResponse(
            List<TopicScoreDto> strongTopics,
            List<TopicScoreDto> averageTopics,
            List<TopicScoreDto> weakTopics,
            List<TopicScoreDto> topics
    ) {}

    public record PredictionHistoryDto(
            LocalDateTime date,
            String subject,
            BigDecimal predicted,
            BigDecimal actual,
            BigDecimal difference
    ) {}

    public record KnowledgeGapDto(
            Long topicId,
            String topicName,
            String subjectName,
            BigDecimal masteryLevel,
            String gapLevel,
            int attemptedQuestions,
            int correctQuestions,
            Integer recommendedHours
    ) {}

    public record WeakSubjectDto(
            Long subjectId,
            String subjectCode,
            String subjectName,
            BigDecimal weaknessScore,
            BigDecimal accuracy,
            int priorityRank
    ) {}

    public record RecommendationDto(
            Long recommendationId,
            String type,
            String title,
            String description,
            String resourceType,
            String resourceUrl,
            int priority,
            String reason,
            boolean viewed
    ) {}

    public record ChatbotRequest(
            String message,
            String provider,
            List<ChatMessage> history
    ) {}

    public record ChatMessage(
            String role,
            String content
    ) {}

    public record ChatbotResponse(
            String reply,
            String intent,
            BigDecimal confidence,
            LocalDateTime timestamp
    ) {}
}
