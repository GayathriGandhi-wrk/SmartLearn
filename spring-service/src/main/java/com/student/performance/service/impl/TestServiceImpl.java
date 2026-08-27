package com.student.performance.service.impl;

import com.student.performance.dto.TestDto;
import com.student.performance.entity.Question;
import com.student.performance.entity.QuestionAttempt;
import com.student.performance.entity.Student;
import com.student.performance.entity.Test;
import com.student.performance.entity.TestQuestion;
import com.student.performance.entity.TestResult;
import com.student.performance.entity.Topic;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.entity.Subject;
import com.student.performance.repository.QuestionAttemptRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.SubjectRepository;
import com.student.performance.repository.TestQuestionRepository;
import com.student.performance.repository.TestRepository;
import com.student.performance.repository.TestResultRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.service.NotificationService;
import com.student.performance.service.TestService;
import com.student.performance.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TestServiceImpl implements TestService {

    private static final int DEFAULT_QUESTION_COUNT = 15;
    private static final int DEFAULT_DURATION = 15;

    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestResultRepository testResultRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public TestServiceImpl(TestRepository testRepository,
                           TestQuestionRepository testQuestionRepository,
                           TestResultRepository testResultRepository,
                           QuestionRepository questionRepository,
                           QuestionAttemptRepository attemptRepository,
                           StudentRepository studentRepository,
                           SubjectRepository subjectRepository,
                           TopicRepository topicRepository,
                           NotificationService notificationService,
                           SecurityUtils securityUtils) {
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.testResultRepository = testResultRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    public TestDto.ActiveTestResponse startTest(Long studentId, TestDto.StartTestRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        // Cancel any in-progress test
        testRepository.findFirstByStudentAndStatusOrderByCreatedAtDesc(student, Test.Status.IN_PROGRESS)
                .ifPresent(t -> {
                    t.setStatus(Test.Status.CANCELLED);
                    testRepository.save(t);
                });

        int questionCount = request.numberOfQuestions() != null ? request.numberOfQuestions() : DEFAULT_QUESTION_COUNT;
        questionCount = Math.min(Math.max(questionCount, 5), 50);
        int duration = request.durationMinutes() != null ? request.durationMinutes() : DEFAULT_DURATION;
        duration = Math.min(Math.max(duration, 5), 120);

        if (request.subjectId() == null) {
            throw new BadRequestException("Please select a subject before starting the test.");
        }
        if (request.topicId() == null) {
            throw new BadRequestException("Please select a topic before starting the test.");
        }
        if (request.difficulty() == null) {
            throw new BadRequestException("Please select a difficulty level before starting the test.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("Please provide a title before starting the test.");
        }

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + request.subjectId()));
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.topicId()));
        if (!topic.getSubject().getSubjectId().equals(subject.getSubjectId())) {
            throw new BadRequestException("Selected topic does not belong to the selected subject.");
        }

        Question.Difficulty difficulty = request.difficulty();

        List<Question> selected = selectAdaptiveQuestions(student, questionCount, subject, topic, difficulty);

        Test test = new Test();
        test.setStudent(student);
        test.setTestType(request.testType() == null ? "ADAPTIVE" : request.testType());
        test.setTitle(request.title().trim());
        test.setDifficultyLevel(difficulty.name());
        test.setTotalQuestions(selected.size());
        test.setTotalMarks(selected.stream().map(Question::getMarks).reduce(BigDecimal.ZERO, BigDecimal::add));
        test.setDurationMinutes(duration);
        test.setStatus(Test.Status.IN_PROGRESS);
        test.setStartedAt(LocalDateTime.now());
        test = testRepository.save(test);

        int order = 1;
        for (Question q : selected) {
            TestQuestion tq = new TestQuestion();
            tq.setTest(test);
            tq.setQuestion(q);
            tq.setOrderNo(order++);
            tq.setAnswered(false);
            testQuestionRepository.save(tq);
        }

        return buildActiveTest(test);
    }

    @Override
    public TestDto.ActiveTestResponse getActiveTest(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Test test = testRepository.findFirstByStudentAndStatusOrderByCreatedAtDesc(student, Test.Status.IN_PROGRESS)
                .orElseThrow(() -> new BadRequestException("No active test found. Start a new test."));
        return buildActiveTest(test);
    }

    @Override
    @Transactional
    public TestDto.TestResultDto submitTest(Long testId, Long studentId, TestDto.SubmitTestRequest request) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
        if (!test.getStudent().getStudentId().equals(studentId)) {
            throw new BadRequestException("This test does not belong to the current student");
        }
        if (test.getStatus() == Test.Status.COMPLETED) {
            throw new BadRequestException("Test already submitted");
        }
        if (test.getStatus() == Test.Status.EXPIRED) {
            throw new BadRequestException("Test has expired");
        }

        Map<Long, TestDto.SubmitAnswerRequest> answerMap = new HashMap<>();
        if (request != null && request.answers() != null) {
            for (TestDto.SubmitAnswerRequest a : request.answers()) {
                answerMap.put(a.testQuestionId(), a);
            }
        }

        List<TestQuestion> tqs = testQuestionRepository.findByTestOrderByOrderNoAsc(test);
        int correct = 0;
        int incorrect = 0;
        int skipped = 0;
        BigDecimal obtained = BigDecimal.ZERO;

        for (TestQuestion tq : tqs) {
            TestDto.SubmitAnswerRequest answer = answerMap.get(tq.getTestQuestionId());
            Question q = tq.getQuestion();
            if (answer != null && answer.selectedAnswer() != null) {
                tq.setSelectedAnswer(answer.selectedAnswer());
                boolean isCorrect = answer.selectedAnswer() == q.getCorrectAnswer();
                tq.setCorrect(isCorrect);
                tq.setMarksObtained(isCorrect ? q.getMarks() : BigDecimal.ZERO);
                tq.setTimeTakenSec(answer.timeTakenSec());
                tq.setAnswered(true);
                if (isCorrect) {
                    correct++;
                    obtained = obtained.add(q.getMarks());
                } else {
                    incorrect++;
                }
                recordAttempt(q, studentId, answer);
            } else {
                tq.setAnswered(false);
                tq.setCorrect(false);
                tq.setMarksObtained(BigDecimal.ZERO);
                skipped++;
            }
            testQuestionRepository.save(tq);
        }

        test.setStatus(Test.Status.COMPLETED);
        test.setSubmittedAt(LocalDateTime.now());
        test.setScore(obtained);
        test.setCorrectCount(correct);
        test.setIncorrectCount(incorrect);
        test.setSkippedCount(skipped);
        BigDecimal pct = test.getTotalMarks().signum() == 0 ? BigDecimal.ZERO
                : obtained.multiply(BigDecimal.valueOf(100)).divide(test.getTotalMarks(), 2, RoundingMode.HALF_UP);
        test.setPercentage(pct);
        testRepository.save(test);

        awardXp(studentId, correct);

        return buildResult(test, tqs);
    }

    @Override
    public TestDto.TestResultDto getResult(Long testId, Long studentId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + testId));
        if (!test.getStudent().getStudentId().equals(studentId)) {
            throw new BadRequestException("This test does not belong to the current student");
        }
        List<TestQuestion> tqs = testQuestionRepository.findByTestOrderByOrderNoAsc(test);
        return buildResult(test, tqs);
    }

    @Override
    public List<TestDto.TestListItem> getTestHistory(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        return testRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(t -> t.getStatus() != Test.Status.CANCELLED)
                .map(t -> new TestDto.TestListItem(
                        t.getTestId(), t.getTitle(), t.getStatus().name(),
                        t.getPercentage(), t.getPercentage() == null ? null : gradeFor(t.getPercentage()),
                        t.getSubmittedAt(), t.getTotalQuestions()))
                .collect(Collectors.toList());
    }

    @Override
    public List<TestDto.ReviewedTestQuestion> getReviewedTestQuestions(Long studentId) {
        List<TestQuestion> tqs = testQuestionRepository
                .findByTest_Student_StudentIdOrderByTest_TestIdDesc(studentId);
        return tqs.stream()
                .filter(tq -> tq.getTest().getStatus() == Test.Status.COMPLETED
                        || tq.getTest().getStatus() == Test.Status.EXPIRED)
                .map(tq -> {
                    Question q = tq.getQuestion();
                    Topic topic = q.getTopic();
                    String subjectName = topic != null && topic.getSubject() != null
                            ? topic.getSubject().getSubjectName() : null;
                    String topicName = topic != null ? topic.getTopicName() : null;
                    return new TestDto.ReviewedTestQuestion(
                            tq.getTest().getTestId(),
                            tq.getTest().getTitle(),
                            subjectName,
                            topicName,
                            q.getDifficulty(),
                            q.getQuestionText(),
                            q.getOptionA(),
                            q.getOptionB(),
                            q.getOptionC(),
                            q.getOptionD(),
                            tq.getSelectedAnswer(),
                            q.getCorrectAnswer(),
                            Boolean.TRUE.equals(tq.getCorrect()),
                            tq.getSelectedAnswer() == null,
                            q.getExplanation());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void expireTimedOutTests() {
        List<Test> active = testRepository.findAll().stream()
                .filter(t -> t.getStatus() == Test.Status.IN_PROGRESS)
                .toList();
        for (Test t : active) {
            if (t.getStartedAt() == null) {
                continue;
            }
            if (t.getStartedAt().plusMinutes(t.getDurationMinutes()).isBefore(LocalDateTime.now())) {
                t.setStatus(Test.Status.EXPIRED);
                t.setSubmittedAt(LocalDateTime.now());
                List<TestQuestion> tqs = testQuestionRepository.findByTestOrderByOrderNoAsc(t);
                int correct = 0, incorrect = 0, skipped = 0;
                for (TestQuestion tq : tqs) {
                    if (tq.getSelectedAnswer() != null) {
                        if (Boolean.TRUE.equals(tq.getCorrect())) {
                            correct++;
                        } else {
                            incorrect++;
                        }
                    } else {
                        skipped++;
                    }
                }
                t.setCorrectCount(correct);
                t.setIncorrectCount(incorrect);
                t.setSkippedCount(skipped);
                t.setScore(BigDecimal.ZERO);
                t.setPercentage(BigDecimal.ZERO);
                testRepository.save(t);
            }
        }
    }

    private List<Question> selectAdaptiveQuestions(Student student, int count, Subject subject, Topic topic,
                                                   Question.Difficulty difficulty) {
        List<Question> selected = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        // 1. Questions from topics with known knowledge gaps (weakest first), scoped to subject/topic/level
        List<Object[]> gaps = attemptRepository.findByStudent(student).stream()
                .filter(a -> a.getQuestion().getTopic() != null
                        && (topic != null || subject == null
                        || subject.getSubjectId().equals(a.getQuestion().getTopic().getSubject().getSubjectId()))
                        && (topic == null || topic.getTopicId().equals(a.getQuestion().getTopic().getTopicId()))
                        && (difficulty == null || a.getQuestion().getDifficulty() == difficulty))
                .collect(Collectors.groupingBy(a -> a.getQuestion().getTopic().getTopicId(),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            long correct = list.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
                            double acc = list.isEmpty() ? 0 : correct * 100.0 / list.size();
                            return new double[]{acc, list.size()};
                        })))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(a.getValue()[0], b.getValue()[0]))
                .map(e -> new Object[]{e.getKey(), e.getValue()[0]})
                .toList();

        int gapQuota = Math.max(2, count / 3);
        for (Object[] gap : gaps) {
            if (selected.size() >= gapQuota) break;
            Long topicId = (Long) gap[0];
            List<Question> pool = difficulty == null
                    ? questionRepository.findRandomByTopic(topicId)
                    : questionRepository.findRandomByTopicAndDifficulty(topicId, difficulty);
            for (Question q : pool) {
                if (!used.add(q.getQuestionId())) continue;
                selected.add(q);
                break;
            }
        }

        // 2. Fill with difficulty-balanced random questions (or only the chosen level)
        List<Question.Difficulty> levels = difficulty == null
                ? List.of(Question.Difficulty.BEGINNER, Question.Difficulty.INTERMEDIATE,
                        Question.Difficulty.ADVANCED, Question.Difficulty.BEGINNER)
                : List.of(difficulty);
        for (Question.Difficulty level : levels) {
            if (selected.size() >= count) break;
            List<Question> pool;
            if (topic != null) {
                pool = difficulty == null
                        ? questionRepository.findRandomByTopic(topic.getTopicId())
                        : questionRepository.findRandomByTopicAndDifficulty(topic.getTopicId(), level);
            } else if (subject == null) {
                pool = questionRepository.findRandomByDifficulty(level);
            } else {
                pool = questionRepository.findRandomBySubjectAndDifficulty(subject.getSubjectId(), level);
            }
            for (Question q : pool) {
                if (selected.size() >= count) break;
                if (used.add(q.getQuestionId())) {
                    selected.add(q);
                }
            }
        }

        // 3. Fallback fill
        for (Question q : questionRepository.findAll()) {
            if (selected.size() >= count) break;
            boolean matchesTopic = topic == null
                    || (q.getTopic() != null && topic.getTopicId().equals(q.getTopic().getTopicId()));
            boolean matchesSubject = subject == null
                    || (q.getTopic() != null && subject.getSubjectId().equals(q.getTopic().getSubject().getSubjectId()));
            boolean matchesDifficulty = difficulty == null || q.getDifficulty() == difficulty;
            if (q.isActive() && matchesTopic && matchesSubject && matchesDifficulty && used.add(q.getQuestionId())) {
                selected.add(q);
            }
        }
        Collections.shuffle(selected);
        return selected;
    }

    private void recordAttempt(Question q, Long studentId, TestDto.SubmitAnswerRequest answer) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return;
        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setStudent(student);
        attempt.setQuestion(q);
        attempt.setSelectedAnswer(answer.selectedAnswer());
        attempt.setCorrect(answer.selectedAnswer() == q.getCorrectAnswer());
        attempt.setTimeTakenSec(answer.timeTakenSec());
        attemptRepository.save(attempt);
    }

    private void awardXp(Long studentId, int correct) {
        Student student = studentRepository.findById(studentId).orElse(null);
        if (student == null) return;
        int xp = correct * 10;
        student.setXpPoints(student.getXpPoints() + xp);
        student.setLevel(student.getXpPoints() / 100 + 1);
        student.setStreakDays(student.getStreakDays() + 1);
        studentRepository.save(student);
        notificationService.notify(student,
                "Test Completed", "You scored " + correct + " correct. +" + xp + " XP earned.", "TEST", null);
    }

    private TestDto.ActiveTestResponse buildActiveTest(Test test) {
        List<TestQuestion> tqs = testQuestionRepository.findByTestOrderByOrderNoAsc(test);
        String subjectName = null;
        Question.Difficulty difficulty = null;
        if (!tqs.isEmpty() && tqs.get(0).getQuestion().getTopic() != null) {
            if (tqs.get(0).getQuestion().getTopic().getSubject() != null) {
                subjectName = tqs.get(0).getQuestion().getTopic().getSubject().getSubjectName();
            }
            difficulty = tqs.get(0).getQuestion().getDifficulty();
        }
        List<TestDto.TestQuestionDto> questions = tqs.stream()
                .map(tq -> new TestDto.TestQuestionDto(
                        tq.getTestQuestionId(), tq.getQuestion().getQuestionId(), tq.getOrderNo(),
                        tq.getQuestion().getQuestionText(), tq.getQuestion().getOptionA(),
                        tq.getQuestion().getOptionB(), tq.getQuestion().getOptionC(),
                        tq.getQuestion().getOptionD(), tq.getQuestion().getDifficulty(),
                        tq.getQuestion().getMarks(), tq.getTimeTakenSec()))
                .collect(Collectors.toList());
        LocalDateTime deadline = test.getStartedAt() == null ? LocalDateTime.now()
                : test.getStartedAt().plusMinutes(test.getDurationMinutes());
        return new TestDto.ActiveTestResponse(test.getTestId(), test.getTitle(), test.getTestType(),
                subjectName, difficulty, test.getTotalQuestions(), test.getTotalMarks(), test.getDurationMinutes(),
                test.getStartedAt(), deadline, questions);
    }

    private TestDto.TestResultDto buildResult(Test test, List<TestQuestion> tqs) {
        List<TestDto.QuestionResult> results = tqs.stream()
                .map(tq -> {
                    Question q = tq.getQuestion();
                    boolean notAttempted = tq.getSelectedAnswer() == null;
                    return new TestDto.QuestionResult(
                            tq.getTestQuestionId(), q.getQuestionId(), q.getQuestionText(),
                            q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                            tq.getSelectedAnswer(), q.getCorrectAnswer(),
                            Boolean.TRUE.equals(tq.getCorrect()), notAttempted,
                            tq.getMarksObtained() == null ? BigDecimal.ZERO : tq.getMarksObtained(),
                            q.getExplanation(), q.getDifficulty());
                })
                .collect(Collectors.toList());

        TestResult tr = testResultRepository.findByStudent_StudentIdOrderByCreatedAtDesc(test.getStudent().getStudentId())
                .stream().filter(r -> r.getTest().getTestId().equals(test.getTestId())).findFirst().orElse(null);

        BigDecimal accuracy = test.getTotalQuestions() == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(test.getCorrectCount() * 100.0 / test.getTotalQuestions()).setScale(2, RoundingMode.HALF_UP);
        String grade = test.getPercentage() == null ? "F" : gradeFor(test.getPercentage());
        String summary = "You answered " + test.getCorrectCount() + " correctly out of " + test.getTotalQuestions()
                + " questions. Score: " + test.getScore() + "/" + test.getTotalMarks()
                + " (" + test.getPercentage() + "%). Grade: " + grade + ".";

        return new TestDto.TestResultDto(
                test.getTestId(), tr == null ? null : tr.getResultId(), test.getTitle(),
                test.getTotalQuestions(), test.getCorrectCount(), test.getIncorrectCount(),
                test.getSkippedCount(), test.getTotalMarks(),
                test.getScore() == null ? BigDecimal.ZERO : test.getScore(),
                test.getPercentage() == null ? BigDecimal.ZERO : test.getPercentage(),
                grade, accuracy, test.getSubmittedAt(), results, summary);
    }

    private String gradeFor(BigDecimal pct) {
        double p = pct.doubleValue();
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B+";
        if (p >= 60) return "B";
        if (p >= 50) return "C";
        if (p >= 40) return "D";
        return "F";
    }
}
