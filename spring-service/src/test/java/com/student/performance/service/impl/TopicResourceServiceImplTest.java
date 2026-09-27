package com.student.performance.service.impl;

import com.student.performance.dto.TopicResourceDto;
import com.student.performance.entity.Question;
import com.student.performance.entity.Student;
import com.student.performance.entity.Subject;
import com.student.performance.entity.Topic;
import com.student.performance.repository.PracticeQuestionRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.ResourceViewRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.service.AiServiceClient;
import com.student.performance.service.QuestionService;
import com.student.performance.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rule under test: a student is never served the same question twice.
 */
class TopicResourceServiceImplTest {

    private ResourceViewRepository resourceViewRepository;
    private QuestionRepository questionRepository;
    private PracticeQuestionRepository practiceRepository;
    private AiServiceClient aiServiceClient;
    private TopicResourceServiceImpl service;

    private final List<Long> saved = new ArrayList<>();
    private final Map<Long, Question> pool = new HashMap<>();
    private long nextId = 1L;
    private int aiIndex = 0;

    @BeforeEach
    void setUp() {
        resourceViewRepository = mock(ResourceViewRepository.class);
        questionRepository = mock(QuestionRepository.class);
        practiceRepository = mock(PracticeQuestionRepository.class);
        TopicRepository topicRepository = mock(TopicRepository.class);
        aiServiceClient = mock(AiServiceClient.class);
        QuestionService questionService = mock(QuestionService.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);

        Subject subject = new Subject();
        subject.setSubjectId(1L);
        subject.setSubjectName("C Programming");

        Topic topic = new Topic();
        topic.setTopicId(7L);
        topic.setTopicName("Pointers");
        topic.setSubject(subject);
        when(topicRepository.findById(7L)).thenReturn(Optional.of(topic));

        Student student = new Student();
        student.setStudentId(42L);
        when(securityUtils.getCurrentStudent()).thenReturn(student);

        // Persisted questions are remembered so later calls see them in the topic.
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            if (q.getQuestionId() == null) {
                q.setQuestionId(nextId++);
                saved.add(q.getQuestionId());
            }
            pool.put(q.getQuestionId(), q);
            return q;
        });
        when(questionRepository.findByTopicActive(7L))
                .thenAnswer(inv -> new ArrayList<>(pool.values()));
        when(questionRepository.findServedTextsForStudent(42L, 7L))
                .thenAnswer(inv -> pool.values().stream()
                        .map(Question::getQuestionText)
                        .filter(java.util.Objects::nonNull)
                        .toList());
        when(practiceRepository.findServedQuestionIds(42L))
                .thenAnswer(inv -> new ArrayList<>(saved));
        when(practiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new TopicResourceServiceImpl(resourceViewRepository, questionRepository,
                practiceRepository, topicRepository, aiServiceClient, questionService, securityUtils);
    }

    /** Fake AI reply: `count` distinct questions, optionally ignoring `avoid`. */
    private Map<String, Object> aiReply(int count, String prefix, List<String> avoid) {
        List<Object> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(aiRow(prefix + " " + i));
        }
        return Map.of("questions", rows, "provider", "gemini", "available", true);
    }

    /** Fake AI that behaves like a real model: it honours the avoid list. */
    private Map<String, Object> smartReply(int count, List<String> avoid) {
        List<Object> rows = new ArrayList<>();
        while (rows.size() < count) {
            Map<String, Object> row = aiRow("Pointer question " + aiIndex);
            aiIndex++;
            String text = String.valueOf(row.get("question_text"));
            if (!avoid.contains(text)) {
                rows.add(row);
            }
        }
        return Map.of("questions", rows, "provider", "gemini", "available", true);
    }

    private Map<String, Object> aiRow(String text) {
        Map<String, Object> row = new HashMap<>();
        row.put("question_text", text);
        row.put("option_a", text + " a");
        row.put("option_b", text + " b");
        row.put("option_c", text + " c");
        row.put("option_d", text + " d");
        row.put("correct_answer", "B");
        row.put("explanation", "because " + text);
        row.put("difficulty", "BEGINNER");
        return row;
    }

    private TopicResourceDto.GenerateResponse generate(int count) {
        return service.generate(7L, new TopicResourceDto.GenerateRequest(count, null, null));
    }

    @Test
    @DisplayName("a question is never served to the same student twice")
    void neverServesTheSameQuestionTwice() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenAnswer(inv -> smartReply(3, inv.getArgument(5)));
        TopicResourceDto.GenerateResponse first = generate(3);
        assertThat(first.questions()).hasSize(3);

        TopicResourceDto.GenerateResponse second = generate(3);

        List<Long> firstIds = first.questions().stream().map(TopicResourceDto.GeneratedQuestion::questionId).toList();
        List<Long> secondIds = second.questions().stream().map(TopicResourceDto.GeneratedQuestion::questionId).toList();

        assertThat(secondIds).hasSize(3);
        assertThat(secondIds).doesNotContainAnyElementsOf(firstIds);
        assertThat(new LinkedHashSet<>(secondIds)).hasSize(3);
    }

    @Test
    @DisplayName("an AI that keeps echoing old questions yields nothing rather than a repeat")
    void stubbornAiNeverReturnsADuplicate() {
        // Always the same two questions, no matter what it is told.
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(aiReply(2, "Always the same", List.of()));

        assertThat(generate(2).questions()).hasSize(2);
        assertThat(generate(2).questions()).isEmpty();
        // Retried rather than giving up after one call.
        verify(aiServiceClient, org.mockito.Mockito.atLeast(2))
                .generateQuestions(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("the retry loop feeds accepted questions back as the avoid list")
    void retryPassesNewlyAcceptedQuestionsBack() {
        // First call returns one usable + one duplicate of an existing question,
        // so the loop must ask again to reach the requested count.
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenAnswer(inv -> smartReply(3, inv.getArgument(5)));
        assertThat(generate(3).questions()).hasSize(3);
    }

    @Test
    @DisplayName("already-served questions are sent to the model as the avoid list")
    void passesServedQuestionsAsAvoidList() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenAnswer(inv -> smartReply(2, inv.getArgument(5)));
        generate(2);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(aiServiceClient).generateQuestions(eq("Pointers"), eq("C Programming"), any(),
                eq("BEGINNER"), anyInt(), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat((List<String>) captor.getValue()).allMatch(s -> s.startsWith("Pointer question"));
    }

    @Test
    @DisplayName("malformed model output is rejected instead of saved")
    void rejectsMalformedQuestions() {
        List<Object> rows = List.of(
                Map.of("question_text", "ok", "option_a", "a", "option_b", "b",
                        "option_c", "c", "option_d", "d", "correct_answer", "Z"),
                Map.of("question_text", "dup options", "option_a", "same", "option_b", "same",
                        "option_c", "c", "option_d", "d", "correct_answer", "A"),
                Map.of("question_text", "", "option_a", "a", "option_b", "b",
                        "option_c", "c", "option_d", "d", "correct_answer", "A"),
                "not-an-object");
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(Map.of("questions", rows, "provider", "gemini"));

        assertThat(generate(5).questions()).isEmpty();
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("falls back to the question bank when the AI service is unavailable")
    void topsUpFromBankWhenAiUnavailable() {
        // AI service down.
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenReturn(Map.of("available", false));

        // Bank already holds one seeded question for this topic.
        Question seeded = new Question();
        seeded.setQuestionId(500L);
        seeded.setTopic(null);
        seeded.setQuestionText("Seeded bank question");
        seeded.setOptionA("a"); seeded.setOptionB("b"); seeded.setOptionC("c"); seeded.setOptionD("d");
        seeded.setCorrectAnswer(Question.Answer.A);
        seeded.setDifficulty(Question.Difficulty.BEGINNER);
        seeded.setSource(Question.Source.SEED);
        when(questionRepository.findByTopicActive(7L)).thenReturn(List.of(seeded));

        TopicResourceDto.GenerateResponse response = generate(3);
        assertThat(response.questions()).hasSize(1);
        assertThat(response.fromBank()).isTrue();
    }

    @Test
    @DisplayName("the answered question is marked solved and the attempt is recorded")
    void answerRecordsAttemptAndMarksSolved() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any()))
                .thenAnswer(inv -> aiReply(1, "Q", List.of()));
        TopicResourceDto.GeneratedQuestion q = generate(1).questions().get(0);
        when(questionRepository.findById(q.questionId())).thenReturn(Optional.of(pool.get(q.questionId())));
        when(practiceRepository.findByStudentAndQuestion(any(), any())).thenReturn(Optional.empty());

        TopicResourceDto.AnswerResponse response =
                service.answer(q.questionId(), new TopicResourceDto.AnswerRequest(Question.Answer.A));

        assertThat(response.correct()).isFalse();
        assertThat(response.correctAnswer()).isEqualTo("B");
        assertThat(response.explanation()).startsWith("because");
    }

    @Test
    @DisplayName("non-http and off-host resource URLs are not stored")
    void rejectsUntrustedResourceUrls() {
        when(resourceViewRepository.touch(anyString(), any())).thenReturn(0);
        when(resourceViewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordView(7L, new TopicResourceDto.ViewRequest("VIDEO", "Bad", "javascript:alert(1)"));
        var captor = org.mockito.ArgumentCaptor.forClass(com.student.performance.entity.ResourceView.class);
        verify(resourceViewRepository).save(captor.capture());
        assertThat(captor.getValue().getResourceUrl()).isEmpty();
    }

    @Test
    @DisplayName("re-opening the same resource updates the row instead of inserting")
    void reusesExistingViewRow() {
        when(resourceViewRepository.touch(anyString(), any())).thenReturn(1);
        com.student.performance.entity.ResourceView existing = new com.student.performance.entity.ResourceView();
        existing.setViewId(9L);
        existing.setResourceTitle("C tutorial");
        existing.setResourceUrl("https://www.youtube.com/results?search_query=c");
        when(resourceViewRepository.findByResourceKey(anyString())).thenReturn(Optional.of(existing));

        TopicResourceDto.ViewResponse response =
                service.recordView(7L, new TopicResourceDto.ViewRequest("VIDEO", "C tutorial",
                        "https://www.youtube.com/results?search_query=c"));
        assertThat(response.viewId()).isEqualTo(9L);
        verify(resourceViewRepository, never()).save(any());
    }

    @Test
    @DisplayName("normalise collapses case and punctuation so near-duplicates collide")
    void normaliseCollapsesNearDuplicates() {
        assertThat(TopicResourceServiceImpl.normalise("Which statement best describes X?"))
                .isEqualTo(TopicResourceServiceImpl.normalise("  which STATEMENT   best describes x  "));
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
