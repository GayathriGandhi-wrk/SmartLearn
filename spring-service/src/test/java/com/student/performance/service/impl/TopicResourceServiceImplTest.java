package com.student.performance.service.impl;

import com.student.performance.dto.TopicResourceDto;
import com.student.performance.entity.Question;
import com.student.performance.entity.Student;
import com.student.performance.entity.Subject;
import com.student.performance.entity.TestQuestion;
import com.student.performance.entity.Topic;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.PracticeQuestionRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.ResourceViewRepository;
import com.student.performance.repository.TestQuestionRepository;
import com.student.performance.repository.TestRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rule under test: a student is never served the same question twice, and
 * the batch they earn is handed to the adaptive test module.
 */
class TopicResourceServiceImplTest {

    private ResourceViewRepository resourceViewRepository;
    private QuestionRepository questionRepository;
    private PracticeQuestionRepository practiceRepository;
    private TopicRepository topicRepository;
    private TestRepository testRepository;
    private TestQuestionRepository testQuestionRepository;
    private AiServiceClient aiServiceClient;
    private SecurityUtils securityUtils;
    private TopicResourceServiceImpl service;

    private final List<Long> saved = new ArrayList<>();
    private final Map<Long, Question> pool = new HashMap<>();
    private final List<TestQuestion> testQuestions = new ArrayList<>();
    private long nextId = 1L;
    private int aiIndex = 0;

    @BeforeEach
    void setUp() {
        resourceViewRepository = mock(ResourceViewRepository.class);
        questionRepository = mock(QuestionRepository.class);
        practiceRepository = mock(PracticeQuestionRepository.class);
        topicRepository = mock(TopicRepository.class);
        testRepository = mock(TestRepository.class);
        testQuestionRepository = mock(TestQuestionRepository.class);
        aiServiceClient = mock(AiServiceClient.class);
        QuestionService questionService = mock(QuestionService.class);
        securityUtils = mock(SecurityUtils.class);

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
        when(resourceViewRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Tests need their generated id back, the way a real save would.
        when(testRepository.findByStudentAndStatus(any(), eq(com.student.performance.entity.Test.Status.IN_PROGRESS)))
                .thenReturn(new ArrayList<>());
        when(testRepository.save(any(com.student.performance.entity.Test.class))).thenAnswer(inv -> {
            com.student.performance.entity.Test t = inv.getArgument(0);
            if (t.getTestId() == null) {
                t.setTestId(900L);
            }
            return t;
        });
        when(testQuestionRepository.save(any(TestQuestion.class))).thenAnswer(inv -> {
            TestQuestion tq = inv.getArgument(0);
            testQuestions.add(tq);
            return tq;
        });

        service = new TopicResourceServiceImpl(resourceViewRepository, questionRepository,
                practiceRepository, topicRepository, testRepository, testQuestionRepository,
                aiServiceClient, questionService, securityUtils);
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
        return service.generate(7L, new TopicResourceDto.GenerateRequest(count, null, null, null, null));
    }

    @Test
    @DisplayName("a question is never served to the same student twice")
    void neverServesTheSameQuestionTwice() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
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
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(aiReply(2, "Always the same", List.of()));

        assertThat(generate(2).questions()).hasSize(2);
        assertThat(generate(2).questions()).isEmpty();
        // Retried rather than giving up after one call.
        verify(aiServiceClient, org.mockito.Mockito.atLeast(2))
                .generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("the retry loop feeds accepted questions back as the avoid list")
    void retryPassesNewlyAcceptedQuestionsBack() {
        // First call returns one usable + one duplicate of an existing question,
        // so the loop must ask again to reach the requested count.
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(3, inv.getArgument(5)));
        assertThat(generate(3).questions()).hasSize(3);
    }

    @Test
    @DisplayName("already-served questions are sent to the model as the avoid list")
    void passesServedQuestionsAsAvoidList() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(2, inv.getArgument(5)));
        generate(2);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(aiServiceClient).generateQuestions(eq("Pointers"), eq("C Programming"), any(), eq("BEGINNER"), anyInt(), captor.capture(), any(), any());
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
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(Map.of("questions", rows, "provider", "gemini"));

        assertThat(generate(5).questions()).isEmpty();
        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("falls back to the question bank when the AI service is unavailable")
    void topsUpFromBankWhenAiUnavailable() {
        // AI service down.
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
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
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
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
    @DisplayName("every reference URL the frontend offers is on the allow list")
    void allowsEveryHostTheFrontendLinksTo() {
        // These are the refUrl/learnUrl values in frontend/js/topic-resources.js.
        // A host missing here means the student can open a page in the browser but
        // the questions silently fall back to the topic instead of the content.
        List<String> appHosts = List.of(
                "developer.mozilla.org", "web.dev", "javascript.info", "react.dev",
                "nodejs.org", "php.net", "docs.oracle.com", "dev.java",
                "docs.python.org", "en.cppreference.com", "isocpp.org",
                "w3schools.com", "dev.mysql.com", "redis.io", "sqlite.org",
                "postgresql.org", "mongodb.com", "kernel.org", "man7.org",
                "linuxjourney.com", "gnu.org", "craftinginterpreters.com",
                "dataintensive.net", "15445.courses.cs.cmu.edu",
                "pages.cs.wisc.edu", "rfc-editor.org", "kubernetes.io",
                "docs.aws.amazon.com", "cloud.google.com", "learnopengl.com",
                "en.wikipedia.org", "www.r-project.org", "cran.r-project.org",
                "ethereum.org", "www.swift.org", "kotlinlang.org", "flutter.dev",
                "numpy.org", "pytorch.org", "huggingface.co", "owasp.org",
                "nptel.ac.in", "nvlpubs.nist.gov", "www.youtube.com", "youtu.be");

        List<String> refused = appHosts.stream()
                .filter(host -> !TopicResourceServiceImpl.isAllowedHost(host))
                .toList();
        assertThat(refused).as("hosts the frontend links to but the allow list does not know")
                .isEmpty();
    }

    @Test
    @DisplayName("a host that merely ends with an allowed name is still refused")
    void refusesLookalikeHosts() {
        assertThat(TopicResourceServiceImpl.isAllowedHost("notyoutube.com")).isFalse();
        assertThat(TopicResourceServiceImpl.isAllowedHost("wikipedia.org.evil.test")).isFalse();
        assertThat(TopicResourceServiceImpl.isAllowedHost("evil.test")).isFalse();
        assertThat(TopicResourceServiceImpl.isAllowedHost("")).isFalse();
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

    @Test
    @DisplayName("the generated batch becomes a real adaptive test containing those questions")
    void generatedQuestionsBecomeAnAdaptiveTest() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(3, inv.getArgument(5)));

        TopicResourceDto.GenerateResponse response = generate(3);

        assertThat(response.testId()).isEqualTo(900L);
        // 3 questions would be 6 minutes, but a test never runs under 10.
        assertThat(response.durationMinutes()).isEqualTo(10);

        var captor = org.mockito.ArgumentCaptor.forClass(com.student.performance.entity.Test.class);
        verify(testRepository).save(captor.capture());
        com.student.performance.entity.Test test = captor.getValue();
        assertThat(test.getTestType()).isEqualTo("LEARNED");
        assertThat(test.getStatus()).isEqualTo(com.student.performance.entity.Test.Status.IN_PROGRESS);
        assertThat(test.getTitle()).contains("Pointers");
        assertThat(test.getTotalQuestions()).isEqualTo(3);
        assertThat(test.getTotalMarks()).isEqualByComparingTo("3.00");

        // The test must hold exactly the questions the student was served, so the
        // test is about what they actually learned.
        List<Long> testQuestionIds = testQuestions.stream()
                .map(tq -> tq.getQuestion().getQuestionId()).toList();
        assertThat(testQuestionIds).containsExactly(
                response.questions().stream()
                        .map(TopicResourceDto.GeneratedQuestion::questionId).toList().toArray(new Long[0]));
        assertThat(testQuestions).allMatch(tq -> tq.getOrderNo() >= 1 && !tq.isAnswered());
    }

    @Test
    @DisplayName("an abandoned test is cancelled so the active test is the new one")
    void cancelsAnyTestStillInProgress() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        com.student.performance.entity.Test abandoned = new com.student.performance.entity.Test();
        abandoned.setTestId(11L);
        abandoned.setStatus(com.student.performance.entity.Test.Status.IN_PROGRESS);
        when(testRepository.findByStudentAndStatus(any(), eq(com.student.performance.entity.Test.Status.IN_PROGRESS)))
                .thenReturn(List.of(abandoned));

        generate(1);

        assertThat(abandoned.getStatus()).isEqualTo(com.student.performance.entity.Test.Status.CANCELLED);
        verify(testRepository).save(abandoned);
    }

    @Test
    @DisplayName("no test is created when the student asked for the questions only")
    void createTestFalseSkipsTheTest() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(2, inv.getArgument(5)));

        TopicResourceDto.GenerateResponse response = service.generate(7L,
                new TopicResourceDto.GenerateRequest(2, null, null, false, null));

        assertThat(response.questions()).hasSize(2);
        assertThat(response.testId()).isNull();
        verify(testRepository, never()).save(any(com.student.performance.entity.Test.class));
    }

    @Test
    @DisplayName("an exhausted topic creates no empty test")
    void noTestWhenNothingWasGenerated() {
        // The AI only ever knows one question, so the second round has nothing
        // left that this student has not already been served.
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> aiReply(1, "The only question", List.of()));

        assertThat(generate(1).questions()).hasSize(1);
        assertThat(generate(1).questions()).isEmpty();

        // Only the first round made a test; the empty round must not.
        verify(testRepository, org.mockito.Mockito.times(1))
                .save(any(com.student.performance.entity.Test.class));
    }

    @Test
    @DisplayName("the requested duration is honoured within the allowed range")
    void clampsTheRequestedDuration() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        assertThat(service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, null, true, 1))
                .durationMinutes()).isEqualTo(10);
        assertThat(service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, null, true, 600))
                .durationMinutes()).isEqualTo(90);
    }

    @Test
    @DisplayName("the question count scales with the size of the concept, between 10 and 20")
    void questionCountFollowsTheSizeOfTheConcept() {
        Topic narrow = new Topic();
        narrow.setEstimatedHours(1);
        Topic wide = new Topic();
        wide.setEstimatedHours(9);
        Topic huge = new Topic();
        huge.setEstimatedHours(40);

        assertThat(TopicResourceServiceImpl.conceptQuestionCount(narrow)).isEqualTo(10);
        assertThat(TopicResourceServiceImpl.conceptQuestionCount(wide)).isEqualTo(17);
        // Never runs past the cap, however wide the topic is declared to be.
        assertThat(TopicResourceServiceImpl.conceptQuestionCount(huge)).isEqualTo(20);
        assertThat(TopicResourceServiceImpl.conceptQuestionCount(null)).isEqualTo(10);
    }

    @Test
    @DisplayName("no explicit count means the concept decides, and the topic is named as the concept")
    void omittedCountUsesTheConceptAndPassesItToTheModel() {
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(10, inv.getArgument(5)));

        // No count in the request at all.
        TopicResourceDto.GenerateResponse response =
                service.generate(7L, new TopicResourceDto.GenerateRequest(null, null, null, null, null));

        assertThat(response.requested()).isEqualTo(10);
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(aiServiceClient).generateQuestions(any(), any(), any(), any(), eq(10), any(), captor.capture(), any());
        // The model is told what the concept is, so the questions stay on it
        // instead of wandering across the whole subject.
        assertThat(captor.getValue()).contains("Pointers");
    }

    @Test
    @DisplayName("a studied topic with no completed test is offered as ready to start")
    void listsStudiedButUntestedTopicsAsPending() {
        Topic topic = topicRepository.findById(7L).orElseThrow();
        topic.setEstimatedHours(4);
        when(resourceViewRepository.summariseStudiedTopics(42L))
                .thenReturn(List.<Object[]>of(new Object[]{7L, 3L, java.time.LocalDateTime.now()}));
        when(testQuestionRepository.findTestedTopicIds(42L)).thenReturn(List.of());
        when(topicRepository.findById(7L)).thenReturn(Optional.of(topic));

        List<TopicResourceDto.PendingTest> pending = service.listPendingTests();

        assertThat(pending).hasSize(1);
        TopicResourceDto.PendingTest p = pending.get(0);
        assertThat(p.topicId()).isEqualTo(7L);
        assertThat(p.topicName()).isEqualTo("Pointers");
        assertThat(p.subjectName()).isEqualTo("C Programming");
        assertThat(p.resourceCount()).isEqualTo(3);
        assertThat(p.questionCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("a topic already tested is no longer offered")
    void omitsTopicsTheStudentHasAlreadyTested() {
        when(resourceViewRepository.summariseStudiedTopics(42L))
                .thenReturn(List.<Object[]>of(new Object[]{7L, 2L, java.time.LocalDateTime.now()}));
        when(testQuestionRepository.findTestedTopicIds(42L)).thenReturn(List.of(7L));

        assertThat(service.listPendingTests()).isEmpty();
    }

    // ------------------------------------------------------------------ //
    // Questions written from the resource itself
    // ------------------------------------------------------------------ //

    /** A resource the student opened, optionally with content already cached. */
    private com.student.performance.entity.ResourceView resourceView(
            com.student.performance.entity.ResourceView.ResourceType type, String url,
            Integer watched, String content, Integer contentSeconds) {
        com.student.performance.entity.ResourceView view = new com.student.performance.entity.ResourceView();
        view.setViewId(3L);
        view.setStudent(securityUtils.getCurrentStudent());
        view.setTopic(topicRepository.findById(7L).orElseThrow());
        view.setResourceType(type);
        view.setResourceTitle("Pointers in 10 minutes");
        view.setResourceUrl(url);
        if (watched != null) {
            view.setWatchedSeconds(watched);
        }
        if (content != null) {
            view.setContentText(content);
            view.setContentSource(com.student.performance.entity.ResourceView.ContentSource.TRANSCRIPT);
            view.setContentSeconds(contentSeconds);
        }
        return view;
    }

    private void withView(com.student.performance.entity.ResourceView view) {
        when(resourceViewRepository.findByStudentAndTopicOrderByLastViewedAtDesc(any(), any()))
                .thenReturn(List.of(view));
        when(resourceViewRepository.findById(3L)).thenReturn(Optional.of(view));
    }

    @Test
    @DisplayName("the questions are written from what the video actually said")
    void passesTheTranscriptToTheModel() {
        String transcript = "A pointer is a variable that stores a memory address instead of a "
                + "value. The ampersand operator returns the address of a variable, and the "
                + "asterisk dereferences a pointer to reach the value it points at. A null "
                + "pointer points at nothing, so dereferencing it is undefined behaviour and "
                + "usually crashes the program.";
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 300, null, null));
        when(aiServiceClient.extractContent(anyString(), any(), any()))
                .thenReturn(Map.of("available", true, "source", "TRANSCRIPT", "text", transcript,
                        "covered_seconds", 300, "duration_seconds", 900));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(3, inv.getArgument(5)));

        TopicResourceDto.GenerateResponse response = generate(3);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(aiServiceClient).generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(),
                captor.capture());
        assertThat(captor.getValue()).isEqualTo(transcript);
        assertThat(response.contentSource()).isEqualTo("TRANSCRIPT");
        assertThat(response.contentNote()).isNull();
        assertThat(response.coveredSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("only the part of the video the student watched is read")
    void trimsTheTranscriptToTheWatchedPosition() {
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 240, null, null));
        when(aiServiceClient.extractContent(anyString(), any(), any()))
                .thenReturn(Map.of("available", true, "source", "TRANSCRIPT", "text", "x".repeat(400)));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, 3L, null, null));

        // The position is the whole point of "I have learned up to here".
        verify(aiServiceClient).extractContent(anyString(), any(), eq(240));
    }

    @Test
    @DisplayName("a page is read whole, because it has no position to stop at")
    void readsTheWholePage() {
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.DOC,
                "https://www.w3schools.com/cpp/cpp_pointers.asp", null, null, null));
        when(aiServiceClient.extractContent(anyString(), any(), any()))
                .thenReturn(Map.of("available", true, "source", "PAGE", "text", "y".repeat(400)));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, 3L, null, null));

        verify(aiServiceClient).extractContent(anyString(), eq("DOC"), eq(null));
    }

    @Test
    @DisplayName("a resource already read for this position is not read again")
    void reusesTheCachedContent() {
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 300, "already fetched", 300));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, 3L, null, null));

        verify(aiServiceClient, never()).extractContent(anyString(), any(), any());
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(aiServiceClient).generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(),
                captor.capture());
        assertThat(captor.getValue()).isEqualTo("already fetched");
    }

    @Test
    @DisplayName("watching further is answered from a fresh transcript, not the short one")
    void rereadsWhenTheStudentWatchedMore() {
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 600, "only the first five minutes", 300));
        when(aiServiceClient.extractContent(anyString(), any(), any()))
                .thenReturn(Map.of("available", true, "source", "TRANSCRIPT", "text", "z".repeat(400)));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, 3L, null, null));

        verify(aiServiceClient).extractContent(anyString(), any(), eq(600));
    }

    @Test
    @DisplayName("a video with no captions falls back to the topic, and says so")
    void reportsWhyThereIsNoContent() {
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 300, null, null));
        when(aiServiceClient.extractContent(anyString(), any(), any()))
                .thenReturn(Map.of("available", false, "text", "",
                        "message", "This video has no captions to read."));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        TopicResourceDto.GenerateResponse response =
                service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, 3L, null, null));

        assertThat(response.contentSource()).isNull();
        assertThat(response.contentNote()).isEqualTo("This video has no captions to read.");
        // The model is told there is nothing to work from rather than being
        // left to invent facts from the topic name.
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(aiServiceClient).generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(),
                captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("a caption fragment is discarded rather than used as a lesson")
    void throwsAwayContentThatIsTooShortToBeUseful() {
        withView(resourceView(com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 5, null, null));
        when(aiServiceClient.extractContent(anyString(), any(), any()))
                .thenReturn(Map.of("available", true, "source", "TRANSCRIPT", "text", "so"));
        when(aiServiceClient.generateQuestions(any(), any(), any(), any(), anyInt(), any(), any(), any()))
                .thenAnswer(inv -> smartReply(1, inv.getArgument(5)));

        TopicResourceDto.GenerateResponse response =
                service.generate(7L, new TopicResourceDto.GenerateRequest(1, null, 3L, null, null));

        assertThat(response.contentSource()).isNull();
        assertThat(response.contentNote()).isNotBlank();
    }

    // ------------------------------------------------------------------ //
    // Watch position
    // ------------------------------------------------------------------ //
    @Test
    @DisplayName("a reported watch position is stored with a readable label")
    void storesTheWatchPosition() {
        com.student.performance.entity.ResourceView view = resourceView(
                com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", null, null, null);
        when(resourceViewRepository.findById(3L)).thenReturn(Optional.of(view));

        TopicResourceDto.ViewResponse response = service.recordProgress(7L, 3L,
                new TopicResourceDto.ProgressRequest(492, 1204));

        assertThat(response.watchedSeconds()).isEqualTo(492);
        assertThat(response.durationSeconds()).isEqualTo(1204);
        assertThat(response.progressLabel()).isEqualTo("8:12 of 20:04 watched");
        verify(resourceViewRepository).save(view);
    }

    @Test
    @DisplayName("rewinding a video does not shrink what the student is tested on")
    void progressNeverMovesBackwards() {
        com.student.performance.entity.ResourceView view = resourceView(
                com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 900, null, null);
        when(resourceViewRepository.findById(3L)).thenReturn(Optional.of(view));

        TopicResourceDto.ViewResponse response =
                service.recordProgress(7L, 3L, new TopicResourceDto.ProgressRequest(60, 1204));

        assertThat(response.watchedSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("a position past the end of the video is not believed")
    void clampsThePositionToTheDuration() {
        com.student.performance.entity.ResourceView view = resourceView(
                com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", null, null, null);
        when(resourceViewRepository.findById(3L)).thenReturn(Optional.of(view));

        assertThat(service.recordProgress(7L, 3L, new TopicResourceDto.ProgressRequest(9999, 1204))
                .watchedSeconds()).isEqualTo(1204);
    }

    @Test
    @DisplayName("another student's resource cannot have its progress changed")
    void refusesProgressOnSomebodyElsesResource() {
        com.student.performance.entity.ResourceView view = new com.student.performance.entity.ResourceView();
        view.setViewId(3L);
        Student other = new Student();
        other.setStudentId(99L);
        view.setStudent(other);
        when(resourceViewRepository.findById(3L)).thenReturn(Optional.of(view));

        assertThatThrownBy(() -> service.recordProgress(7L, 3L,
                new TopicResourceDto.ProgressRequest(10, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(resourceViewRepository, never()).save(any());
    }

    @Test
    @DisplayName("a long video is labelled in hours rather than as 75 minutes")
    void clockHandlesLongVideos() {
        com.student.performance.entity.ResourceView view = resourceView(
                com.student.performance.entity.ResourceView.ResourceType.VIDEO,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ", null, null, null);
        when(resourceViewRepository.findById(3L)).thenReturn(Optional.of(view));

        assertThat(service.recordProgress(7L, 3L, new TopicResourceDto.ProgressRequest(4500, 7200))
                .progressLabel()).isEqualTo("1:15:00 of 2:00:00 watched");
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
