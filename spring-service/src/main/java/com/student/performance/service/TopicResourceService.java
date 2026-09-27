package com.student.performance.service;

import com.student.performance.dto.TopicResourceDto;

import java.util.List;

public interface TopicResourceService {

    /** Records that the student opened a tutorial/doc/video for a topic. */
    TopicResourceDto.ViewResponse recordView(Long topicId, TopicResourceDto.ViewRequest request);

    /** Resources this student has already opened, most recent first. */
    List<TopicResourceDto.ViewResponse> listViews(Long topicId);

    /**
     * "I have learned up to here": generates questions for what the student has
     * just studied. A question is never returned to the same student twice.
     */
    TopicResourceDto.GenerateResponse generate(Long topicId, TopicResourceDto.GenerateRequest request);

    /** Records an answer to a generated question and returns the correct answer. */
    TopicResourceDto.AnswerResponse answer(Long questionId, TopicResourceDto.AnswerRequest request);
}
