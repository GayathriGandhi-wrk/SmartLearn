package com.student.performance.service;

import com.student.performance.dto.TopicResourceDto;

import java.util.List;

public interface TopicResourceService {

    /** Records that the student opened a tutorial/doc/video for a topic. */
    TopicResourceDto.ViewResponse recordView(Long topicId, TopicResourceDto.ViewRequest request);

    /** Resources this student has already opened, most recent first. */
    List<TopicResourceDto.ViewResponse> listViews(Long topicId);

    /**
     * Stores how far into a video the student has watched, so generation only
     * quizzes the part they actually saw. Positions never move backwards, so
     * rewinding to rewatch something does not shrink the test.
     */
    TopicResourceDto.ViewResponse recordProgress(Long topicId, Long viewId,
                                                TopicResourceDto.ProgressRequest request);

    /**
     * Concepts this student has studied through videos or sites but has not
     * been tested on yet, most recent first. Feeds the "ready to start" list on
     * the adaptive test page.
     */
    List<TopicResourceDto.PendingTest> listPendingTests();

    /**
     * "I have learned up to here": generates questions for what the student has
     * just studied. A question is never returned to the same student twice.
     *
     * <p>The questions are written from the text of the resource itself - video
     * captions up to the watched position, or the readable text of a page - so
     * they follow the lesson rather than the topic name. When no content can be
     * read the response says so and the questions fall back to the concept.
     */
    TopicResourceDto.GenerateResponse generate(Long topicId, TopicResourceDto.GenerateRequest request);

    /** Records an answer to a generated question and returns the correct answer. */
    TopicResourceDto.AnswerResponse answer(Long questionId, TopicResourceDto.AnswerRequest request);
}
