package com.student.performance.service.impl;

import com.student.performance.dto.TopicResourceDto;
import com.student.performance.entity.PracticeQuestion;
import com.student.performance.entity.Question;
import com.student.performance.entity.ResourceView;
import com.student.performance.entity.Student;
import com.student.performance.entity.Test;
import com.student.performance.entity.TestQuestion;
import com.student.performance.entity.Topic;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.PracticeQuestionRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.ResourceViewRepository;
import com.student.performance.repository.TestQuestionRepository;
import com.student.performance.repository.TestRepository;
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
import java.util.Objects;
import java.util.Set;

/**
 * Tracks the tutorials a student opens and turns that into a quiz via the AI
 * service. Two rules govern the whole class.
 *
 * <p>The first is that a student is never shown the same question twice. That is
 * enforced two ways:
 *
 * <ul>
 *   <li>every question handed out gets a practice_questions row, so anything
 *       already served to this student is excluded from the next batch;</li>
 *   <li>every question carries a SHA-256 of its normalised text, so a
 *       regenerated near-duplicate is rejected before it can enter the pool.</li>
 * </ul>
 *
 * <p>The second is that the questions must be about the lesson, not the subject.
 * A topic name is far too little to write a question from, so before generating
 * anything this class asks the AI service to read the resource itself - the
 * captions of a video up to the point the student had played, or the readable
 * text of a page - and hands that text to the model as the only permitted source
 * of facts. The extract is cached on the resource_views row so it is read once.
 * When nothing can be read (a video with no captions, a site that refuses
 * automated requests) the questions fall back to the topic and the response
 * carries the reason, so a generic test is never passed off as one taken from
 * the video.
 *
 * <p>The generated batch is then wrapped in a real {@code tests} row, so the
 * student takes it in the Adaptive Test module rather than in a panel on the
 * learning page. That is deliberate: the test module already owns grading, the
 * countdown, XP and the test history, so the learning page only has to hand the
 * questions over.
 *
 * <p>When the AI service cannot supply enough fresh questions (no API key, or the
 * topic bank is exhausted) the remaining slots are filled from the seeded bank,
 * still skipping anything this student has already been served.
 */
@Service
public class TopicResourceServiceImpl implements TopicResourceService {

    private static final Logger log = LoggerFactory.getLogger(TopicResourceServiceImpl.class);
    private static final int MAX_COUNT = 20;
    private static final int MIN_CONCEPT_COUNT = 10;
    private static final int MAX_AI_ATTEMPTS = 3;
    /** Marks a test the student did not configure by hand, but earned by learning. */
    private static final String LEARNED_TEST_TYPE = "LEARNED";
    private static final int MINUTES_PER_QUESTION = 2;
    private static final int MIN_DURATION = 10;
    private static final int MAX_DURATION = 90;
    /** A resource is read once and reused, so the prompt is never left unbounded. */
    private static final int MAX_CONTENT_CHARS = 12000;
    /** Below this a "transcript" is a caption fragment, not a lesson. */
    private static final int MIN_CONTENT_CHARS = 200;
    /**
     * The documentation hosts the learning pages actually send a student to.
     *
     * <p>Two things depend on this list. A URL outside it is not stored, so the
     * student gets no record of what they studied; and it is not fetched, so
     * there is no text to write the questions from. Anything a video or a page
     * has to be read from therefore has to be here, which is why it covers every
     * host {@code topic-resources.js} builds a link for rather than a handful of
     * favourites.
     *
     * <p>A host matches on its own name or as a subdomain of an entry, so
     * {@code docs.aws.amazon.com} and {@code en.wikipedia.org} are covered by
     * {@code aws.amazon.com} and {@code wikipedia.org} without opening the door
     * to the rest of those domains.
     */
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            // Video
            "youtube.com", "youtu.be",
            // The general reference sites
            "w3schools.com", "developer.mozilla.org", "wikipedia.org", "google.com",
            "geeksforgeeks.org", "javatpoint.com", "tutorialspoint.com", "coursera.org",
            // Language documentation
            "docs.oracle.com", "dev.java", "learn.microsoft.com", "php.net", "python.org",
            "kotlinlang.org", "swift.org", "ruby-lang.org", "scala-lang.org",
            "cppreference.com", "cplusplus.com", "gnu.org", "nasm.us", "man7.org",
            "kernel.org", "typescriptlang.org", "javascript.info",
            // Web, backend and frontend frameworks
            "nodejs.org", "expressjs.com", "django-project.com", "djangoproject.com",
            "palletsprojects.com", "rubyonrails.org", "laravel.com", "spring.io",
            "react.dev", "vuejs.org", "angular.dev", "flutter.dev", "android.com",
            "bootstrap.com", "tailwindcss.com",
            // Data, databases and cloud
            "postgresql.org", "mysql.com", "mongodb.com", "redis.io", "sqlite.org",
            "apache.org", "aws.amazon.com", "cloud.google.com", "azure.microsoft.com",
            "docker.com", "kubernetes.io", "helm.sh", "terraform.io",
            // Data science, ML and AI
            "numpy.org", "scipy.org", "pandas.pydata.org",
            "matplotlib.org", "scikit-learn.org", "pytorch.org", "tensorflow.org",
            "huggingface.co", "kaggle.com", "opencv.org",
            "developers.google.com", "spark.apache.org", "databricks.com",
            // Security
            "owasp.org", "mitre.org", "nvd.nist.gov",
            // Systems, networking and architecture
            "rfc-editor.org", "ietf.org", "w3.org",
            "riscv.org", "visualgo.net", "craftinginterpreters.com",
            "dataintensive.net", "roadmap.sh", "git-scm.com", "github.com",
            // Engineering practice
            "ieee.org", "acm.org", "iso.org", "nist.gov",
            // University and reference material
            "cmu.edu", "wisc.edu", "mit.edu", "stanford.edu", "berkeley.edu",
            "ocw.mit.edu", "15445.courses.cs.cmu.edu", "pages.cs.wisc.edu",
            // Other subjects
            "khanacademy.org", "allaboutcircuits.com", "investopedia.com",
            "accountingcoach.com", "marketingexamples.com", "toastmasters.org",
            "digitalforensicsmagazine.com", "linuxjourney.com", "wooledge.org",
            "programiz.com", "nptel.ac.in", "nvlpubs.nist.gov",
            "r-project.org", "ethereum.org", "isocpp.org", "learnopengl.com", "web.dev");

    private final ResourceViewRepository resourceViewRepository;
    private final QuestionRepository questionRepository;
    private final PracticeQuestionRepository practiceRepository;
    private final TopicRepository topicRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AiServiceClient aiServiceClient;
    private final QuestionService questionService;
    private final SecurityUtils securityUtils;

    public TopicResourceServiceImpl(ResourceViewRepository resourceViewRepository,
                                    QuestionRepository questionRepository,
                                    PracticeQuestionRepository practiceRepository,
                                    TopicRepository topicRepository,
                                    TestRepository testRepository,
                                    TestQuestionRepository testQuestionRepository,
                                    AiServiceClient aiServiceClient,
                                    QuestionService questionService,
                                    SecurityUtils securityUtils) {
        this.resourceViewRepository = resourceViewRepository;
        this.questionRepository = questionRepository;
        this.practiceRepository = practiceRepository;
        this.topicRepository = topicRepository;
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
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

    @Override
    @Transactional
    public TopicResourceDto.ViewResponse recordProgress(Long topicId, Long viewId,
                                                       TopicResourceDto.ProgressRequest request) {
        Student student = securityUtils.getCurrentStudent();
        requireTopic(topicId);
        ResourceView view = resourceViewRepository.findById(viewId)
                .filter(v -> student.getStudentId().equals(v.getStudent().getStudentId()))
                .orElseThrow(() -> new ResourceNotFoundException("Resource view not found: " + viewId));

        Integer duration = positive(request == null ? null : request.durationSeconds());
        Integer watched = clampToDuration(positive(request == null ? null : request.watchedSeconds()), duration);
        if (watched == null) {
            throw new BadRequestException("watchedSeconds must be a positive number of seconds");
        }

        boolean changed = false;
        if (duration != null && !duration.equals(view.getDurationSeconds())) {
            view.setDurationSeconds(duration);
            changed = true;
        }
        // Only ever moves forward. A student who rewinds to rewatch a hard
        // section has not unlearnt the rest, and shrinking the mark here would
        // quietly shrink the test they are about to be given.
        if (view.getWatchedSeconds() == null || watched > view.getWatchedSeconds()) {
            view.setWatchedSeconds(watched);
            changed = true;
        }
        String label = progressLabel(view.getWatchedSeconds(), view.getDurationSeconds());
        if (!label.equals(view.getProgressLabel())) {
            view.setProgressLabel(label);
            changed = true;
        }
        if (!changed) {
            return TopicResourceDto.toViewResponse(view);
        }
        return TopicResourceDto.toViewResponse(resourceViewRepository.save(view));
    }

    // ------------------------------------------------------------------ //
    // Concepts waiting to be tested
    // ------------------------------------------------------------------ //
    @Override
    @Transactional(readOnly = true)
    public List<TopicResourceDto.PendingTest> listPendingTests() {
        Student student = securityUtils.getCurrentStudent();
        Set<Long> tested = new HashSet<>(testQuestionRepository.findTestedTopicIds(student.getStudentId()));

        List<TopicResourceDto.PendingTest> pending = new ArrayList<>();
        for (Object[] row : resourceViewRepository.summariseStudiedTopics(student.getStudentId())) {
            Long topicId = (Long) row[0];
            if (topicId == null || tested.contains(topicId)) {
                continue;
            }
            Topic topic = topicRepository.findById(topicId).orElse(null);
            if (topic == null) {
                continue;
            }
            int resourceCount = row[1] == null ? 0 : ((Number) row[1]).intValue();
            pending.add(new TopicResourceDto.PendingTest(
                    topicId,
                    topic.getTopicName(),
                    topic.getSubject() == null ? "" : topic.getSubject().getSubjectName(),
                    topic.getDifficultyLevel(),
                    conceptQuestionCount(topic),
                    resourceCount,
                    String.valueOf(row[2])));
        }
        return pending;
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
                ? new TopicResourceDto.GenerateRequest(null, null, null, null, null)
                : request;

        int count = req.count() == null ? conceptQuestionCount(topic) : req.count();
        count = clamp(count, 1, MAX_COUNT);
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
        // What the resource actually said, up to the point the student reached.
        // This is what the questions get written from; the topic description is
        // only the fallback for when a video has no captions or a site refuses
        // to be read.
        ResourceContent content = resolveContent(view);
        // The topic description is the closest thing we have to a statement of
        // the concept, so it is what the model is told to test.
        String concept = conceptFor(topic, resourceTitle);

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
                    difficulty.name(), count - served.size(), servedTexts, concept,
                    content.text());
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
        log.debug("Generated {} question(s) for topic {} (provider={}, fromBank={}, content={})",
                served.size(), topicId, provider, fromBank,
                content.source() == null ? "none" : content.source().name());

        // 4) Hand the batch to the Adaptive Test module. The questions are
        //    already the ones the student "learned", so re-running the adaptive
        //    selector here would throw that away and ask about something else.
        boolean wantsTest = !Boolean.FALSE.equals(req.createTest());
        Test test = wantsTest && !served.isEmpty()
                ? createAdaptiveTest(student, topic, served, difficulty, resourceTitle, req.durationMinutes())
                : null;

        return new TopicResourceDto.GenerateResponse(
                topicId,
                topic.getTopicName(),
                subjectName,
                resourceTitle,
                provider,
                fromBank,
                count,
                served.stream().map(TopicResourceServiceImpl::toGenerated).toList(),
                test == null ? null : test.getTestId(),
                test == null ? null : test.getDurationMinutes(),
                content.source() == null ? null : content.source().name(),
                content.note(),
                content.coveredSeconds());
    }

    // ------------------------------------------------------------------ //
    // What the resource actually said
    // ------------------------------------------------------------------ //

    /**
     * Reads the text the student consumed: the captions of a video up to the
     * point they reached, or the readable text of a page.
     *
     * <p>A stored extract is only reused for the exact position it was read at,
     * so watching further is never answered from a stale short transcript. The
     * fetch is best effort - no captions, or a site that refuses automated
     * requests, simply means the questions fall back to the concept, and the
     * reason is passed back so the UI can say so rather than passing a generic
     * test off as one taken from the video.
     */
    private ResourceContent resolveContent(ResourceView view) {
        if (view == null) {
            return ResourceContent.none("No resource was recorded for this attempt.");
        }
        String url = view.getResourceUrl();
        if (url == null || url.isBlank()) {
            return ResourceContent.none("That resource had no usable link to read.");
        }

        // How much of a video counts as learned. Null means "all of it", which
        // is also what a page always means since a page has no position.
        Integer covered = view.getResourceType() == ResourceView.ResourceType.VIDEO
                ? view.getWatchedSeconds()
                : null;

        if (isReusable(view, covered)) {
            return new ResourceContent(view.getContentText(), view.getContentSource(),
                    covered, null);
        }

        Map<String, Object> response =
                aiServiceClient.extractContent(url, typeName(view), covered);
        if (response == null) {
            response = Map.of();
        }
        String text = truncate(text(response.get("text")), MAX_CONTENT_CHARS);
        if (text.length() < MIN_CONTENT_CHARS) {
            // A stub of a few words would only mislead the model, so it is
            // thrown away and the student is told why.
            return ResourceContent.none(fallbackNote(response));
        }

        view.setContentText(text);
        view.setContentSource(parseContentSource(text(response.get("source"))));
        // Recorded as the position that was asked for, not the one that came
        // back, so the same request reuses this extract and a different one
        // does not.
        view.setContentSeconds(covered);
        if (view.getDurationSeconds() == null) {
            view.setDurationSeconds(positive(asInt(response.get("duration_seconds"))));
        }
        resourceViewRepository.save(view);

        Integer actual = positive(asInt(response.get("covered_seconds")));
        return new ResourceContent(text, view.getContentSource(), actual, null);
    }

    /** True when the stored text was read for exactly the position being asked about. */
    private boolean isReusable(ResourceView view, Integer covered) {
        if (view.getContentText() == null || view.getContentText().isBlank()) {
            return false;
        }
        return Objects.equals(view.getContentSeconds(), covered);
    }

    /** The reason the AI service gave for having nothing, phrased for a student. */
    private String fallbackNote(Map<String, Object> response) {
        String message = text(response == null ? null : response.get("message"));
        return message.isEmpty()
                ? "The resource could not be read, so the questions cover the topic instead."
                : truncate(message, 200);
    }

    private String typeName(ResourceView view) {
        return view.getResourceType() == null ? null : view.getResourceType().name();
    }

    private ResourceView.ContentSource parseContentSource(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ResourceView.ContentSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * The text of a resource, once it has been read.
     *
     * @param coveredSeconds the point in a video the text actually reaches, null
     *                       when it covers the whole resource
     * @param note           why there is no text, when there is none
     */
    private record ResourceContent(
            String text,
            ResourceView.ContentSource source,
            Integer coveredSeconds,
            String note) {

        static ResourceContent none(String note) {
            return new ResourceContent("", null, null, note);
        }
    }

    // ------------------------------------------------------------------ //
    // Concept scope
    // ------------------------------------------------------------------ //

    /**
     * How many questions a concept deserves, between 10 and 20.
     *
     * <p>{@code estimated_hours} is the only measure of how broad a topic is
     * that the schema already carries, so the test scales with it: a two-hour
     * topic gets the minimum, a wide one gets the full twenty. A fixed count
     * would either under-test the big topics or make the small ones tedious.
     */
    static int conceptQuestionCount(Topic topic) {
        int hours = topic == null ? 2 : topic.getEstimatedHours();
        return Math.max(MIN_CONCEPT_COUNT, Math.min(MAX_COUNT, hours + 8));
    }

    /**
     * What the questions should be about. The topic name always anchors it, the
     * description says what the concept covers, and the resource the student
     * actually worked through narrows it further. Never empty for a real topic,
     * because an unnamed concept is what lets the model drift.
     */
    private String conceptFor(Topic topic, String resourceTitle) {
        String name = topic == null || topic.getTopicName() == null ? "" : topic.getTopicName().trim();
        String description = topic == null || topic.getDescription() == null
                ? "" : topic.getDescription().trim();

        StringBuilder concept = new StringBuilder(name);
        if (!description.isEmpty()) {
            concept.append(": ").append(description);
        }
        if (resourceTitle != null && !resourceTitle.isBlank()) {
            concept.append(" (from the resource: ").append(resourceTitle.trim()).append(")");
        }
        return truncate(concept.toString(), 500);
    }

    // ------------------------------------------------------------------ //
    // Adaptive test hand-off
    // ------------------------------------------------------------------ //

    /**
     * Wraps the freshly generated questions in a real test row so the student
     * takes them in the Adaptive Test module: countdown, navigation, grading,
     * XP and a row in the test history, all for free.
     *
     * <p>Any test still in progress is cancelled first. That mirrors
     * {@code TestServiceImpl.startTest} and is what keeps
     * {@code GET /tests/active} unambiguous - the resume button on the test
     * page must always point at the test the student just generated, never at
     * one they abandoned earlier.
     */
    private Test createAdaptiveTest(Student student, Topic topic, List<Question> questions,
                                    Question.Difficulty difficulty, String resourceTitle,
                                    Integer requestedDuration) {
        for (Test open : testRepository.findByStudentAndStatus(student, Test.Status.IN_PROGRESS)) {
            open.setStatus(Test.Status.CANCELLED);
            testRepository.save(open);
        }

        int duration = requestedDuration == null
                ? Math.max(MIN_DURATION, questions.size() * MINUTES_PER_QUESTION)
                : clamp(requestedDuration, MIN_DURATION, MAX_DURATION);

        Test test = new Test();
        test.setStudent(student);
        // LEARNED keeps it distinguishable from a test the student configured
        // by hand, which is what the "based on what you learned" label keys off.
        test.setTestType(LEARNED_TEST_TYPE);
        test.setTitle(truncate(learnedTestTitle(topic, resourceTitle), 150));
        test.setDifficultyLevel(difficulty.name());
        test.setTotalQuestions(questions.size());
        test.setTotalMarks(questions.stream()
                .map(Question::getMarks)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        test.setDurationMinutes(duration);
        test.setStatus(Test.Status.IN_PROGRESS);
        test.setStartedAt(LocalDateTime.now());
        test = testRepository.save(test);

        int order = 1;
        for (Question q : questions) {
            TestQuestion tq = new TestQuestion();
            tq.setTest(test);
            tq.setQuestion(q);
            tq.setOrderNo(order++);
            tq.setAnswered(false);
            testQuestionRepository.save(tq);
        }

        log.debug("Created learned-check test {} with {} question(s) for topic {}",
                test.getTestId(), questions.size(), topic.getTopicId());
        return test;
    }

    /** Names the test after the topic, mentioning the resource when we know it. */
    private String learnedTestTitle(Topic topic, String resourceTitle) {
        String name = topic.getTopicName() == null ? "Topic" : topic.getTopicName().trim();
        if (resourceTitle == null || resourceTitle.isBlank()) {
            return "Learned check: " + name;
        }
        return "Learned check: " + name + " (" + resourceTitle.trim() + ")";
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
            return isAllowedHost(java.net.URI.create(value).getHost()) ? value : "";
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    /** True for a host on the allow list, including any subdomain of one. */
    static boolean isAllowedHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String value = host.trim().toLowerCase(Locale.ROOT);
        for (String allowed : ALLOWED_HOSTS) {
            if (value.equals(allowed) || value.endsWith("." + allowed)) {
                return true;
            }
        }
        return false;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** A whole number, or null when the value is missing or not one. */
    private static Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** A strictly positive number of seconds, or null. */
    private static Integer positive(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    /** A player that reports a position past the end of its own video is ignored. */
    private static Integer clampToDuration(Integer seconds, Integer duration) {
        if (seconds == null || duration == null) {
            return seconds;
        }
        return Math.min(seconds, duration);
    }

    /** "8:12 of 20:04 watched", the one line the student sees next to the button. */
    private static String progressLabel(Integer watched, Integer duration) {
        if (watched == null) {
            return null;
        }
        String head = clock(watched);
        return duration == null ? head + " watched" : head + " of " + clock(duration) + " watched";
    }

    private static String clock(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int rest = seconds % 60;
        return hours > 0
                ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, rest)
                : String.format(Locale.ROOT, "%d:%02d", minutes, rest);
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
