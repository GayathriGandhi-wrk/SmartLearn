package com.student.performance.dto;

import java.time.LocalDate;
import java.util.List;

public class PlannerDto {

    public record TimeSlot(
            boolean enabled,
            String start,
            String end
    ) {}

    public record AvailableTime(
            TimeSlot morning,
            TimeSlot afternoon,
            TimeSlot evening,
            TimeSlot night
    ) {}

    public record SubjectSelection(
            Long subjectId,
            boolean allTopics,
            List<Long> topics
    ) {}

    public record StudyPlanRequest(
            String planType,
            Integer totalHoursPerDay,
            AvailableTime availableTime,
            List<SubjectSelection> subjects
    ) {}

    public record SessionDto(
            Long sessionId,
            LocalDate date,
            String day,
            String startTime,
            String endTime,
            int durationMinutes,
            String subject,
            String topic,
            String difficulty,
            String priority,
            String activity,
            String status
    ) {}

    public record DistributionEntry(
            String name,
            double hours
    ) {}

    public record PriorityTopicDto(
            String subject,
            String topic,
            String priority,
            String difficulty,
            double recommendedHours,
            String reason,
            String activity
    ) {}

    public record StudyPlanSummaryDto(
            Long planId,
            String planType,
            LocalDate startDate,
            LocalDate endDate,
            int totalMinutes,
            double totalHours,
            int subjectCount,
            int topicCount,
            int sessionCount,
            int highPriorityCount,
            int completedSessions,
            int completionPercent,
            List<SessionDto> sessions,
            List<DistributionEntry> subjectDistribution,
            List<DistributionEntry> topicDistribution,
            List<PriorityTopicDto> priorityTopics,
            List<PriorityTopicDto> deferredTopics
    ) {}
}
