package com.student.performance.dto;

import com.student.performance.entity.Question;
import com.student.performance.entity.ResourceView;

import java.util.List;

public class TopicResourceDto {

    public record ViewRequest(
            String resourceType,
            String resourceTitle,
            String resourceUrl
    ) {}

    public record ViewResponse(
            Long viewId,
            Long topicId,
            String resourceType,
            String resourceTitle,
            String resourceUrl,
            String progressLabel,
            int viewCount,
            String firstViewedAt,
            String lastViewedAt
    ) {}

    public record GenerateRequest(
            Integer count,
            String difficulty,
            Long resourceViewId
    ) {}

    /** A generated question. correctAnswer is only present after the student answers. */
    public record GeneratedQuestion(
            Long questionId,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String difficulty,
            String source,
            boolean answered,
            String selectedAnswer,
            String correctAnswer,
            boolean correct,
            String explanation
    ) {}

    public record GenerateResponse(
            Long topicId,
            String topicName,
            String subjectName,
            String resourceTitle,
            String provider,
            boolean fromBank,
            int requested,
            List<GeneratedQuestion> questions
    ) {}

    public record AnswerRequest(
            Question.Answer selectedAnswer
    ) {}

    public record AnswerResponse(
            Long questionId,
            String correctAnswer,
            String explanation,
            boolean correct
    ) {}

    public static ViewResponse toViewResponse(ResourceView view) {
        return new ViewResponse(
                view.getViewId(),
                view.getTopic() == null ? null : view.getTopic().getTopicId(),
                view.getResourceType() == null ? null : view.getResourceType().name(),
                view.getResourceTitle(),
                view.getResourceUrl(),
                view.getProgressLabel(),
                view.getViewCount(),
                String.valueOf(view.getFirstViewedAt()),
                String.valueOf(view.getLastViewedAt()));
    }
}
