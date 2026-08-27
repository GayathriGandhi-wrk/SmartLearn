package com.student.performance.dto;

import com.student.performance.entity.Question;

import java.math.BigDecimal;

public class QuestionDto {

    public record QuestionRequest(
            Long topicId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Answer correctAnswer,
            String explanation,
            Question.Difficulty difficulty,
            BigDecimal marks,
            String questionType
    ) {}

    public record QuestionResponse(
            Long questionId,
            Long topicId,
            String topicName,
            Long subjectId,
            String subjectName,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Difficulty difficulty,
            String explanation,
            BigDecimal marks,
            String questionType
    ) {}

    public record QuestionBankResponse(
            long totalQuestions,
            long beginnerCount,
            long intermediateCount,
            long advancedCount,
            java.util.List<QuestionResponse> questions
    ) {}

    public record PracticeResponse(
            Long practiceId,
            Long questionId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            Question.Difficulty difficulty,
            String explanation,
            boolean bookmarked,
            boolean solved,
            int attemptCount
    ) {}

    public record AttemptRequest(
            Question.Answer selectedAnswer,
            Integer timeTakenSec
    ) {}
}
