package com.student.performance.dto;

import com.student.performance.entity.Question;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TestDto {

    public record StartTestRequest(
            Integer numberOfQuestions,
            Integer durationMinutes,
            String testType,
            String title,
            Long subjectId,
            Long topicId,
            Question.Difficulty difficulty
    ) {}

    public record TestQuestionDto(
            Long testQuestionId,
            Long questionId,
            int orderNo,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Difficulty difficulty,
            BigDecimal marks,
            Integer timeTakenSec
    ) {}

    public record ActiveTestResponse(
            Long testId,
            String title,
            String testType,
            String subjectName,
            Question.Difficulty difficulty,
            int totalQuestions,
            BigDecimal totalMarks,
            int durationMinutes,
            LocalDateTime startedAt,
            LocalDateTime deadline,
            List<TestQuestionDto> questions
    ) {}

    public record SubmitAnswerRequest(
            Long testQuestionId,
            Question.Answer selectedAnswer,
            Integer timeTakenSec
    ) {}

    public record SubmitTestRequest(
            List<SubmitAnswerRequest> answers
    ) {}

    public record QuestionResult(
            Long testQuestionId,
            Long questionId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Answer selectedAnswer,
            Question.Answer correctAnswer,
            boolean isCorrect,
            boolean notAttempted,
            BigDecimal marksObtained,
            String explanation,
            Question.Difficulty difficulty
    ) {}

    public record TestResultDto(
            Long testId,
            Long resultId,
            String title,
            int totalQuestions,
            int correctCount,
            int incorrectCount,
            int skippedCount,
            BigDecimal totalMarks,
            BigDecimal obtainedMarks,
            BigDecimal percentage,
            String grade,
            BigDecimal accuracyRate,
            LocalDateTime submittedAt,
            List<QuestionResult> questions,
            String summaryText
    ) {}

    public record TestListItem(
            Long testId,
            String title,
            String status,
            BigDecimal percentage,
            String grade,
            LocalDateTime submittedAt,
            int totalQuestions
    ) {}

    public record ReviewedTestQuestion(
            Long testId,
            String testTitle,
            String subjectName,
            String topicName,
            Question.Difficulty difficulty,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Answer selectedAnswer,
            Question.Answer correctAnswer,
            boolean correct,
            boolean notAttempted,
            String explanation
    ) {}
}
