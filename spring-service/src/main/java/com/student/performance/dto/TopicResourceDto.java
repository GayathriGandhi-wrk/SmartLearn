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
            String lastViewedAt,
            Integer watchedSeconds,
            Integer durationSeconds
    ) {}

    /**
     * How far the student got in a video, reported by the player.
     *
     * <p>Generation reads this back so "I have learned up to here" only quizzes
     * the part that was actually watched. Positions never move backwards: a
     * student who rewinds and re-watches is not asking to be tested on less.
     */
    public record ProgressRequest(
            Integer watchedSeconds,
            Integer durationSeconds
    ) {}

    /**
     * @param createTest        when true (the default) the generated questions are
     *                          packed into a real adaptive test, so the student
     *                          takes them in the Adaptive Test module instead of
     *                          an inline panel. Set false to only persist them.
     * @param durationMinutes   optional test duration. Defaults to two minutes a
     *                          question, clamped to the test module's range.
     */
    public record GenerateRequest(
            Integer count,
            String difficulty,
            Long resourceViewId,
            Boolean createTest,
            Integer durationMinutes
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

    /**
     * @param testId           id of the adaptive test created from these questions,
     *                         or null when the student asked not to create one.
     * @param durationMinutes  duration stamped on that test, so the test module can
     *                         show the right clock without another round trip.
     * @param contentSource    TRANSCRIPT or PAGE when the questions were written
     *                         from what the resource actually said, null when no
     *                         content could be read and the questions are about
     *                         the topic alone.
     * @param contentNote      why there is no content, when there is none. Shown
     *                         to the student so a generic test is never passed
     *                         off as one taken from the video.
     * @param coveredSeconds   the point in a video the questions cover, so the
     *                         test page can show how much of it was studied.
     */
    public record GenerateResponse(
            Long topicId,
            String topicName,
            String subjectName,
            String resourceTitle,
            String provider,
            boolean fromBank,
            int requested,
            List<GeneratedQuestion> questions,
            Long testId,
            Integer durationMinutes,
            String contentSource,
            String contentNote,
            Integer coveredSeconds
    ) {}

    public record AnswerRequest(
            Question.Answer selectedAnswer
    ) {}

    /**
     * A concept the student has worked through but has not been tested on yet,
     * so the adaptive test page can offer it as ready to start.
     *
     * @param questionCount how many questions the test for this concept will hold
     * @param resourceCount how many videos or sites they opened for it
     */
    public record PendingTest(
            Long topicId,
            String topicName,
            String subjectName,
            String difficultyLevel,
            int questionCount,
            int resourceCount,
            String lastViewedAt
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
                String.valueOf(view.getLastViewedAt()),
                view.getWatchedSeconds(),
                view.getDurationSeconds());
    }
}
