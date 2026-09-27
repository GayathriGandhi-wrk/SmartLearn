package com.student.performance.service.impl;

import com.student.performance.dto.TopicResourceDto;
import com.student.performance.entity.PracticeQuestion;
import com.student.performance.entity.Question;
import com.student.performance.entity.ResourceView;
import com.student.performance.entity.Student;
import com.student.performance.entity.Topic;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.PracticeQuestionRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.ResourceViewRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.service.AiServiceClient;
import com.student.performance.service.QuestionService;
import com.student.performance.service.TopicResourceService;
import com.student.performance.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the tutorials a student opens and turns that into a quiz via the AI
 * service. The rule the whole class exists for: a student is never shown the
 * same question twice. That is enforced two ways:
 *
 * <ul>
 *   <li>every question handed out gets a practice_questions row, so anything
 *       already served to this student is excluded from the next batch;</li>
 *   <li>every question carries a SHA-256 of its normalised text, so a
 *       regenerated near-duplicate is rejected before it can enter the pool.</li>
 * </ul>
 *
 * When the AI service cannot supply enough fresh questions (no API key, or the
 * topic bank is exhausted) the remaining slots are filled from the seeded bank,
 * still skipping anything this student has already been served.
 */
@Service
public class TopicResourceServiceImpl implements TopicResourceService {

    private static final Logger log = LoggerFactory.getLogger(TopicResourceServiceImpl.class);
    private static final int MAX_COUNT = 10;
    private static final int MAX_AI_ATTEMPTS = 3;
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "youtu.be", "www.youtu.be",
            "w3schools.com", "www.w3schools.com",
            "developer.mozilla.org", "www.developer.mozilla.org",
            "www.geeksforgeeks.org", "geeksforgeeks.org",
            "www.javatpoint.com", "javatpoint.com",
            "www.tutorialspoint.com", "tutorialspoint.com",
            "en.wikipedia.org", "wikipedia.org",
            "www.coursera.org", "coursera.org",
            "docs.oracle.com", "www.google.com", "google.com");

    private final ResourceViewRepository resourceViewRepository;
    private final QuestionRepository questionRepository;
    private final PracticeQuestionRepository practiceRepository;
    private final TopicRepository topicRepository;
    private final AiServiceClient aiServiceClient;
    private final QuestionService questionService;
    private final SecurityUtils securityUtils;

    public TopicResourceServiceImpl(ResourceViewRepository resourceViewRepository,
                                    QuestionRepository questionRepository,
                                    PracticeQuestionRepository practiceRepository,
                                    TopicRepository topicRepository,
                                    AiServiceClient aiServiceClient,
                                    QuestionService questionService,
                                    SecurityUtils securityUtils) {
        this.resourceViewRepository = resourceViewRepository;
        this.questionRepository = questionRepository;
        this.practiceRepository = practiceRepository;
        this.topicRepository = topicRepository;
        this.aiServiceClient = aiServiceClient;
        this.questionService = questionService;
        this.securityUtils = securityUtils;
    }

    // ------------------------------------------------------------------ //
    // Resource views
    // ------------------------------------------------------------------ //
    @Override
    @Transactional
    public TopicResourceDto.ViewResponse recordView(Long topicId, TopicResourceDto.ViewRequest request) {
        if (request == null || request.resourceTitle() == null || request.resourceTitle().isBlank()) {
            throw new BadRequestException("resourceTitle is required");
        }
        Student student = securityUtils.getCurrentStudent();
        Topic topic = requireTopic(topicId);

        String url = truncate(sanitiseUrl(request.resourceUrl()), 500);
        ResourceView.ResourceType type = parseType(request.resourceType());
        String title = truncate(request.resourceTitle().trim(), 255);
        String key = hash(student.getStudentId() + "|" + topicId + "|" + url);

        if (resourceViewRepository.touch(key, LocalDateTime.now()) > 0) {
            return resourceViewRepository.findByResourceKey(key)
                    .map(TopicResourceDto::toViewResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("Resource view not found"));
        }

        ResourceView view = new ResourceView();
        view.setStudent(student);
        view.setTopic(topic);
        view.setResourceType(type);
        view.setResourceTitle(title);
        view.setResourceUrl(url);
        view.setResourceKey(key);
        try {
            return TopicResourceDto.toViewResponse(resourceViewRepository.save(view));
        } catch (DataIntegrityViolationException ex) {
            // Two clicks on the same link raced; the first one already stored it.
            return resourceViewRepository.findByResourceKey(key)
                    .map(TopicResourceDto::toViewResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("Resource view not found"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicResourceDto.ViewResponse> listViews(Long topicId) {
        Student student = securityUtils.getCurrentStudent();
        Topic topic = requireTopic(topicId);
        return resourceViewRepository.findByStudentAndTopicOrderByLastViewedAtDesc(student, topic)
                .stream()
                .map(TopicResourceDto::toViewResponse)
                .toList();
    }

    // ------------------------------------------------------------------ //
    // Generation
    // ------------------------------------------------------------------ //
    @Override
    @Transactional
    public TopicResourceDto.GenerateResponse generate(Long topicId, TopicResourceDto.GenerateRequest request) {
        Student student = securityUtils.getCurrentStudent();
        Topic topic = requireTopic(topicId);
        TopicResourceDto.GenerateRequest req = request == null
                ? new TopicResourceDto.GenerateRequest(null, null, null)
                : request;

        int count = clamp(req.count() == null ? 5 : req.count(), 1, MAX_COUNT);
        // Fall back to the topic's own level, then BEGINNER, so a request without
        // an explicit difficulty can never produce a null.
        Question.Difficulty difficulty = parseDifficulty(req.difficulty());
        if (difficulty == null) {
            difficulty = parseDifficulty(topic.getDifficultyLevel());
        }
        if (difficulty == null) {
            difficulty = Question.Difficulty.BEGINNER;
        }

        // Everything this student has already been served in this topic.
        List<String> servedTexts = new ArrayList<>(
                questionRepository.findServedTextsForStudent(student.getStudentId(), topicId));
        Set<String> blocked = new HashSet<>();
        servedTexts.forEach(text -> blocked.add(normalise(text)));
        Set<Long> servedIds = new HashSet<>(practiceRepository.findServedQuestionIds(student.getStudentId()));

        ResourceView view = resolveView(student, topic, req.resourceViewId());
        String resourceTitle = view == null ? "" : view.getResourceTitle();
        String subjectName = topic.getSubject() == null ? "" : topic.getSubject().getSubjectName();

        List<Question> served = new ArrayList<>();

        // Existing questions in this topic, so we never duplicate a seeded one.
        List<Question> topicQuestions = questionRepository.findByTopicActive(topicId);
        Set<String> topicTexts = new HashSet<>();
        for (Question existing : topicQuestions) {
            topicTexts.add(normalise(existing.getQuestionText()));
        }

        // 1) Ask the AI service. It sometimes echoes the avoid list back at us, so
        //    retry with the updated avoid list before giving up on the AI.
        Map<String, Object> response = null;
        for (int attempt = 0; attempt < MAX_AI_ATTEMPTS && served.size() < count; attempt++) {
            response = aiServiceClient.generateQuestions(
                    topic.getTopicName(), subjectName, resourceTitle,
                    difficulty.name(), count - served.size(), servedTexts);
            for (Object row : rowsOf(response)) {
                if (served.size() >= count) {
                    break;
                }
                Question q = toQuestion(row, topic, difficulty, resourceTitle);
                if (q == null) {
                    continue;
                }
                String normalised = normalise(q.getQuestionText());
                if (blocked.contains(normalised) || topicTexts.contains(normalised)) {
                    continue;
                }
                String hash = hash(q.getQuestionText());
                if (questionRepository.existsByContentHash(hash)) {
                    continue;
                }
                q.setContentHash(hash);
                Question saved = questionRepository.save(q);
                topicTexts.add(normalised);
                served.add(saved);
                // Feed it back so a retry cannot return the same text.
                servedTexts.add(q.getQuestionText());
            }
        }

        // 2) Top up from the seeded bank so the student always gets a full batch.
        if (served.size() < count) {
            for (Question q : topicQuestions) {
                if (served.size() >= count) {
                    break;
                }
                if (servedIds.contains(q.getQuestionId())
                        || blocked.contains(normalise(q.getQuestionText()))) {
                    continue;
                }
                served.add(q);
            }
        }
        boolean fromBank = !served.isEmpty()
                && served.stream().noneMatch(q -> q.getSource() == Question.Source.AI);

        // 3) Mark everything as served so it is never offered to this student again.
        for (Question q : served) {
            if (!servedIds.contains(q.getQuestionId())) {
                practiceRepository.save(newPractice(student, q));
            }
        }

        String provider = response == null ? "unavailable" : String.valueOf(response.getOrDefault("provider", "unknown"));
        log.debug("Generated {} question(s) for topic {} (provider={}, fromBank={})",
                served.size(), topicId, provider, fromBank);

        return new TopicResourceDto.GenerateResponse(
                topicId,
                topic.getTopicName(),
                subjectName,
                resourceTitle,
                provider,
                fromBank,
                count,
                served.stream().map(TopicResourceServiceImpl::toGenerated).toList());
    }

    // ------------------------------------------------------------------ //
    // Answering
    // ------------------------------------------------------------------ //
    @Override
    @Transactional
    public TopicResourceDto.AnswerResponse answer(Long questionId, TopicResourceDto.AnswerRequest request) {
        if (request == null || request.selectedAnswer() == null) {
            throw new BadRequestException("selectedAnswer is required");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        boolean correct = question.getCorrectAnswer() == request.selectedAnswer();
        // recordAttempt persists the attempt, marks the practice row solved and
        // bumps the attempt counter, so it feeds the existing analytics too.
        questionService.recordAttempt(questionId, request.selectedAnswer(), null);

        return new TopicResourceDto.AnswerResponse(
                questionId,
                question.getCorrectAnswer().name(),
                question.getExplanation(),
                correct);
    }

    // ------------------------------------------------------------------ //
    // Helpers
    // ------------------------------------------------------------------ //
    private Topic requireTopic(Long topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
    }

    private ResourceView resolveView(Student student, Topic topic, Long viewId) {
        if (viewId != null) {
            return resourceViewRepository.findById(viewId)
                    .filter(v -> student.getStudentId().equals(v.getStudent().getStudentId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Resource view not found: " + viewId));
        }
        List<ResourceView> recent =
                resourceViewRepository.findByStudentAndTopicOrderByLastViewedAtDesc(student, topic);
        return recent.isEmpty() ? null : recent.get(0);
    }

    private PracticeQuestion newPractice(Student student, Question question) {
        PracticeQuestion practice = new PracticeQuestion();
        practice.setStudent(student);
        practice.setQuestion(question);
        return practice;
    }

    @SuppressWarnings("unchecked")
    private List<Object> rowsOf(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object questions = response.get("questions");
        return questions instanceof List ? (List<Object>) questions : List.of();
    }

    /** Validates the model output and maps it onto our entity. Returns null when unusable. */
    private Question toQuestion(Object row, Topic topic, Question.Difficulty fallbackDifficulty,
                                String resourceTitle) {
        if (!(row instanceof Map<?, ?> map)) {
            return null;
        }
        String text = text(map.get("question_text"));
        String a = text(map.get("option_a"));
        String b = text(map.get("option_b"));
        String c = text(map.get("option_c"));
        String d = text(map.get("option_d"));
        String answer = text(map.get("correct_answer")).toUpperCase(Locale.ROOT);

        if (text.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            return null;
        }
        if (new HashSet<>(List.of(a, b, c, d)).size() != 4) {
            return null;
        }
        Question.Answer correct;
        try {
            correct = Question.Answer.valueOf(answer);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
        Question.Difficulty difficulty = parseDifficulty(text(map.get("difficulty")));

        Question q = new Question();
        q.setTopic(topic);
        q.setQuestionText(truncate(text, 2000));
        q.setOptionA(truncate(a, 500));
        q.setOptionB(truncate(b, 500));
        q.setOptionC(truncate(c, 500));
        q.setOptionD(truncate(d, 500));
        q.setCorrectAnswer(correct);
        q.setExplanation(truncate(text(map.get("explanation")), 2000));
        q.setDifficulty(difficulty == null ? fallbackDifficulty : difficulty);
        q.setMarks(marksFor(difficulty == null ? fallbackDifficulty : difficulty));
        q.setQuestionType("MCQ");
        q.setActive(true);
        q.setSource(Question.Source.AI);
        q.setSourceResource(truncate(resourceTitle, 255));
        return q;
    }

    private BigDecimal marksFor(Question.Difficulty difficulty) {
        return switch (difficulty) {
            case INTERMEDIATE -> BigDecimal.valueOf(2);
            case ADVANCED -> BigDecimal.valueOf(3);
            default -> BigDecimal.ONE;
        };
    }

    private Question.Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Question.Difficulty.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ResourceView.ResourceType parseType(String value) {
        if (value == null || value.isBlank()) {
            return ResourceView.ResourceType.VIDEO;
        }
        try {
            return ResourceView.ResourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ResourceView.ResourceType.VIDEO;
        }
    }

    /** Only stores http(s) URLs pointing at the resource hosts we link to. */
    private String sanitiseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String value = url.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "";
        }
        try {
            String host = java.net.URI.create(value).getHost();
            return (host != null && ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT))) ? value : "";
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Lowercase, strip punctuation and collapse whitespace so near-duplicates collide. */
    static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    static TopicResourceDto.GeneratedQuestion toGenerated(Question q) {
        return new TopicResourceDto.GeneratedQuestion(
                q.getQuestionId(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD(),
                q.getDifficulty().name(),
                q.getSource() == null ? "SEED" : q.getSource().name(),
                false,
                null,
                null,
                false,
                null);
    }
}
