package com.student.performance.service.impl;

import com.student.performance.dto.AdminDto;
import com.student.performance.entity.Question;
import com.student.performance.entity.QuestionAttempt;
import com.student.performance.entity.Student;
import com.student.performance.entity.Subject;
import com.student.performance.entity.Test;
import com.student.performance.entity.Topic;
import com.student.performance.entity.User;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.PredictionRepository;
import com.student.performance.repository.QuestionAttemptRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.SubjectRepository;
import com.student.performance.repository.TestRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.repository.UserRepository;
import com.student.performance.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final StudentRepository studentRepository;
    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final PredictionRepository predictionRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionAttemptRepository attemptRepository;

    public AdminServiceImpl(StudentRepository studentRepository,
                            QuestionRepository questionRepository,
                            TestRepository testRepository,
                            PredictionRepository predictionRepository,
                            UserRepository userRepository,
                            TopicRepository topicRepository,
                            SubjectRepository subjectRepository,
                            QuestionAttemptRepository attemptRepository) {
        this.studentRepository = studentRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.predictionRepository = predictionRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.attemptRepository = attemptRepository;
    }

    @Override
    public AdminDto.DashboardStats getDashboardStats() {
        long students = studentRepository.count();
        long questions = questionRepository.count();
        long tests = testRepository.count();
        long predictions = predictionRepository.count();
        long users = userRepository.count();
        long subjects = subjectRepository.count();
        long topics = topicRepository.count();

        long highRisk = predictionRepository.findAll().stream()
                .filter(p -> p.getRiskLevel() == com.student.performance.entity.Prediction.RiskLevel.HIGH
                        || p.getRiskLevel() == com.student.performance.entity.Prediction.RiskLevel.CRITICAL)
                .count();

        Map<String, Long> riskDist = predictionRepository.findAll().stream()
                .collect(Collectors.groupingBy(p -> p.getRiskLevel().name(), Collectors.counting()));

        List<Map<String, Object>> recentTests = testRepository.findAll().stream()
                .filter(t -> t.getSubmittedAt() != null)
                .sorted(Comparator.comparing(Test::getSubmittedAt).reversed())
                .limit(5)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("studentName", t.getStudent() != null && t.getStudent().getUser() != null
                            ? t.getStudent().getUser().getFullName() : "-");
                    m.put("title", t.getTitle());
                    m.put("percentage", t.getPercentage());
                    m.put("submittedAt", t.getSubmittedAt());
                    return m;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> subjectWise = buildSubjectWise();

        double avg = testRepository.findAll().stream()
                .filter(t -> t.getPercentage() != null)
                .mapToDouble(t -> t.getPercentage().doubleValue())
                .average().orElse(0);

        return new AdminDto.DashboardStats(
                students, questions, tests, predictions,
                Math.round(avg * 100.0) / 100.0, highRisk, users,
                subjects, topics, recentTests, subjectWise, riskDist);
    }

    private List<Map<String, Object>> buildSubjectWise() {
        Map<Long, long[]> acc = new LinkedHashMap<>();
        for (QuestionAttempt a : attemptRepository.findAll()) {
            if (a.getQuestion() == null || a.getQuestion().getTopic() == null
                    || a.getQuestion().getTopic().getSubject() == null) {
                continue;
            }
            Long sid = a.getQuestion().getTopic().getSubject().getSubjectId();
            long[] v = acc.computeIfAbsent(sid, k -> new long[2]);
            v[0]++;
            if (Boolean.TRUE.equals(a.getCorrect())) v[1]++;
        }
        return acc.entrySet().stream()
                .map(e -> {
                    Subject s = subjectRepository.findById(e.getKey()).orElse(null);
                    if (s == null) return null;
                    long total = e.getValue()[0];
                    long correct = e.getValue()[1];
                    double avg = total > 0 ? Math.round(correct * 10000.0 / total) / 100.0 : 0;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("subjectName", s.getSubjectName());
                    m.put("subject", s.getSubjectName());
                    m.put("avgScore", avg);
                    m.put("averageScore", avg);
                    return m;
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(m -> (String) m.get("subjectName")))
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminDto.UserResponse> listUsers(String role) {
        return userRepository.findAll().stream()
                .filter(u -> role == null || role.isBlank() || u.getRole().getRoleName().equalsIgnoreCase(role))
                .map(u -> {
                    Long studentId = studentRepository.findByUser_UserId(u.getUserId())
                            .map(Student::getStudentId).orElse(null);
                    return new AdminDto.UserResponse(
                            u.getUserId(), u.getEmail(), u.getFullName(), u.getPhone(),
                            u.getRole().getRoleName(), u.isActive(), u.isVerified(), u.getProfileImage(),
                            studentId);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDto.StudentOverviewResponse getStudentOverview(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        User user = student.getUser();

        List<Test> tests = testRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(t -> t.getStatus() == Test.Status.COMPLETED && t.getPercentage() != null)
                .collect(Collectors.toList());

        List<Map<String, Object>> history = tests.stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("testId", t.getTestId());
                    m.put("title", t.getTitle());
                    m.put("percentage", t.getPercentage());
                    m.put("score", t.getScore());
                    m.put("totalMarks", t.getTotalMarks());
                    m.put("correctCount", t.getCorrectCount());
                    m.put("incorrectCount", t.getIncorrectCount());
                    m.put("skippedCount", t.getSkippedCount());
                    m.put("totalQuestions", t.getTotalQuestions());
                    m.put("grade", t.getPercentage() == null ? "-" : gradeFor(t.getPercentage()));
                    m.put("submittedAt", t.getSubmittedAt());
                    return m;
                })
                .collect(Collectors.toList());

        double avg = tests.stream().mapToDouble(t -> t.getPercentage().doubleValue()).average().orElse(0);
        double best = tests.stream().mapToDouble(t -> t.getPercentage().doubleValue()).max().orElse(0);
        double latest = tests.isEmpty() ? 0 : tests.get(0).getPercentage().doubleValue();

        List<Map<String, Object>> subjectPerf = buildStudentSubjectWise(student);
        List<Map<String, Object>> topicPerf = buildStudentTopicWise(student);

        return new AdminDto.StudentOverviewResponse(
                user.getUserId(), student.getStudentId(), user.getFullName(), user.getEmail(),
                user.getPhone(), student.getStudentCode(), student.getDepartment(), student.getSemester(),
                student.getCgpa() == null ? null : student.getCgpa().doubleValue(),
                student.getLevel(), student.getXpPoints(), student.getStreakDays(),
                student.getEnrollmentYear(), student.getBatch(), student.getAddress(),
                student.getGender() == null ? null : student.getGender().name(),
                student.getDateOfBirth() == null ? null : student.getDateOfBirth().toString(),
                user.isActive(), user.isVerified(),
                (long) tests.size(),
                Math.round(avg * 100.0) / 100.0,
                Math.round(best * 100.0) / 100.0,
                Math.round(latest * 100.0) / 100.0,
                history, subjectPerf, topicPerf);
    }

    private List<Map<String, Object>> buildStudentSubjectWise(Student student) {
        Map<Long, long[]> acc = new LinkedHashMap<>();
        for (QuestionAttempt a : attemptRepository.findByStudent(student)) {
            if (a.getQuestion() == null || a.getQuestion().getTopic() == null
                    || a.getQuestion().getTopic().getSubject() == null) continue;
            Long sid = a.getQuestion().getTopic().getSubject().getSubjectId();
            long[] v = acc.computeIfAbsent(sid, k -> new long[2]);
            v[0]++;
            if (Boolean.TRUE.equals(a.getCorrect())) v[1]++;
        }
        return acc.entrySet().stream()
                .map(e -> {
                    Subject s = subjectRepository.findById(e.getKey()).orElse(null);
                    if (s == null) return null;
                    long total = e.getValue()[0];
                    long correct = e.getValue()[1];
                    double avg = total > 0 ? Math.round(correct * 10000.0 / total) / 100.0 : 0;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("subject", s.getSubjectName());
                    m.put("accuracy", avg);
                    m.put("attempts", total);
                    return m;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildStudentTopicWise(Student student) {
        Map<Long, long[]> acc = new LinkedHashMap<>();
        for (QuestionAttempt a : attemptRepository.findByStudent(student)) {
            if (a.getQuestion() == null || a.getQuestion().getTopic() == null) continue;
            Long tid = a.getQuestion().getTopic().getTopicId();
            long[] v = acc.computeIfAbsent(tid, k -> new long[2]);
            v[0]++;
            if (Boolean.TRUE.equals(a.getCorrect())) v[1]++;
        }
        return acc.entrySet().stream()
                .map(e -> {
                    Topic t = topicRepository.findById(e.getKey()).orElse(null);
                    if (t == null) return null;
                    long total = e.getValue()[0];
                    long correct = e.getValue()[1];
                    double avg = total > 0 ? Math.round(correct * 10000.0 / total) / 100.0 : 0;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("topic", t.getTopicName());
                    m.put("accuracy", avg);
                    m.put("attempts", total);
                    return m;
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(m -> (Double) m.get("accuracy")))
                .collect(Collectors.toList());
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

    @Override
    @Transactional
    public long importQuestions(List<AdminDto.QuestionImportItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("No questions provided");
        }
        long count = 0;
        for (AdminDto.QuestionImportItem item : items) {
            Topic topic = topicRepository.findById(item.topicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + item.topicId()));
            Question q = new Question();
            q.setTopic(topic);
            q.setQuestionText(item.questionText());
            q.setOptionA(item.optionA());
            q.setOptionB(item.optionB());
            q.setOptionC(item.optionC());
            q.setOptionD(item.optionD());
            try {
                q.setCorrectAnswer(Question.Answer.valueOf(item.correctAnswer().toUpperCase()));
            } catch (Exception ex) {
                throw new BadRequestException("Invalid correct answer: " + item.correctAnswer());
            }
            q.setExplanation(item.explanation());
            q.setDifficulty(Question.Difficulty.valueOf(item.difficulty().toUpperCase()));
            q.setMarks(item.marks() == null ? BigDecimal.ONE : BigDecimal.valueOf(item.marks()));
            q.setQuestionType("MCQ");
            q.setActive(true);
            questionRepository.save(q);
            count++;
        }
        return count;
    }
}
