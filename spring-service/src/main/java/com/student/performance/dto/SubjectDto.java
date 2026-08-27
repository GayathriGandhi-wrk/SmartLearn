package com.student.performance.dto;

import com.student.performance.entity.Question;
import com.student.performance.entity.Subject;
import com.student.performance.entity.Topic;

import java.math.BigDecimal;
import java.util.List;

public class SubjectDto {

    public record SubjectResponse(
            Long subjectId,
            String subjectCode,
            String subjectName,
            String department,
            Integer semester,
            BigDecimal creditHours,
            String description,
            long topicCount,
            long questionCount
    ) {
        public static SubjectResponse from(Subject s, long topics, long questions) {
            return new SubjectResponse(s.getSubjectId(), s.getSubjectCode(), s.getSubjectName(),
                    s.getDepartment(), s.getSemester(), s.getCreditHours(), s.getDescription(), topics, questions);
        }
    }

    public record TopicResponse(
            Long topicId,
            Long subjectId,
            String subjectName,
            String topicName,
            String difficultyLevel,
            String description,
            int estimatedHours
    ) {
        public static TopicResponse from(Topic t) {
            return new TopicResponse(t.getTopicId(), t.getSubject().getSubjectId(),
                    t.getSubject().getSubjectName(), t.getTopicName(), t.getDifficultyLevel(),
                    t.getDescription(), t.getEstimatedHours());
        }
    }

    public record SubjectRequest(
            String subjectCode,
            String subjectName,
            String department,
            Integer semester,
            BigDecimal creditHours,
            String description
    ) {}

    public record TopicRequest(
            String topicName,
            String difficultyLevel,
            String description,
            Integer estimatedHours
    ) {}

    public record SubjectMarksRequest(
            Long subjectId,
            BigDecimal marks,
            BigDecimal attendance,
            BigDecimal assignments,
            BigDecimal internalMarks
    ) {}

    public record SubjectPerformanceResponse(
            Long subjectId,
            String subjectCode,
            String subjectName,
            long attempts,
            long correctAnswers,
            BigDecimal accuracy,
            BigDecimal averageMarks,
            BigDecimal attendance,
            BigDecimal assignmentScore,
            BigDecimal internalMarks,
            String status
    ) {}

    public record QuestionPerformance(
            Question.Answer selectedAnswer,
            boolean correct,
            BigDecimal marksObtained,
            Integer timeTakenSec,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Answer correctAnswer,
            String explanation,
            Question.Difficulty difficulty
    ) {}

    public record PerformanceResponse(
            List<SubjectPerformanceResponse> subjects,
            List<QuestionPerformance> recentAttempts,
            BigDecimal overallAccuracy
    ) {}
}
