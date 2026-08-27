package com.student.performance.service.impl;

import com.student.performance.dto.SubjectDto;
import com.student.performance.entity.Student;
import com.student.performance.entity.Subject;
import com.student.performance.entity.Topic;
import com.student.performance.entity.WeakSubject;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.DuplicateResourceException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.QuestionAttemptRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.SubjectRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.repository.WeakSubjectRepository;
import com.student.performance.service.SubjectService;
import com.student.performance.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final WeakSubjectRepository weakSubjectRepository;
    private final StudentRepository studentRepository;
    private final SecurityUtils securityUtils;

    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              TopicRepository topicRepository,
                              QuestionRepository questionRepository,
                              QuestionAttemptRepository attemptRepository,
                              WeakSubjectRepository weakSubjectRepository,
                              StudentRepository studentRepository,
                              SecurityUtils securityUtils) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.weakSubjectRepository = weakSubjectRepository;
        this.studentRepository = studentRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public List<SubjectDto.SubjectResponse> listSubjects(String department) {
        List<Subject> subjects = department != null && !department.isBlank()
                ? subjectRepository.findByDepartmentIgnoreCaseAndActiveTrue(department)
                : subjectRepository.findByActiveTrue();
        return subjects.stream()
                .map(s -> {
                    long topics = topicRepository.findBySubject(s).size();
                    long questions = questionRepository.findByTopic_Subject_SubjectId(s.getSubjectId()).stream()
                            .filter(q -> q.isActive()).count();
                    return SubjectDto.SubjectResponse.from(s, topics, questions);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubjectDto.SubjectResponse createSubject(SubjectDto.SubjectRequest request) {
        if (request.subjectCode() == null || request.subjectCode().isBlank()
                || request.subjectName() == null || request.subjectName().isBlank()) {
            throw new BadRequestException("Subject code and name are required");
        }
        String code = request.subjectCode().trim().toUpperCase();
        if (subjectRepository.existsBySubjectCode(code)) {
            throw new DuplicateResourceException("Subject code already exists: " + code);
        }
        Subject subject = new Subject();
        subject.setSubjectCode(code);
        subject.setSubjectName(request.subjectName().trim());
        subject.setDepartment(request.department() == null || request.department().isBlank()
                ? "CSE" : request.department().trim());
        subject.setSemester(request.semester() == null ? 1 : request.semester());
        subject.setCreditHours(request.creditHours() == null
                ? new BigDecimal("3.0") : request.creditHours());
        subject.setDescription(request.description());
        subject.setActive(true);
        Subject saved = subjectRepository.save(subject);
        return SubjectDto.SubjectResponse.from(saved, 0, 0);
    }

    @Override
    @Transactional
    public SubjectDto.TopicResponse createTopic(Long subjectId, SubjectDto.TopicRequest request) {
        if (request.topicName() == null || request.topicName().isBlank()) {
            throw new BadRequestException("Topic name is required");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setTopicName(request.topicName().trim());
        topic.setDifficultyLevel(request.difficultyLevel() == null || request.difficultyLevel().isBlank()
                ? "BEGINNER" : request.difficultyLevel().trim().toUpperCase());
        topic.setDescription(request.description());
        topic.setEstimatedHours(request.estimatedHours() == null ? 2 : request.estimatedHours());
        return SubjectDto.TopicResponse.from(topicRepository.save(topic));
    }

    @Override
    public List<SubjectDto.TopicResponse> listTopics(Long subjectId) {
        subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
        return topicRepository.findBySubjectSubjectId(subjectId).stream()
                .map(SubjectDto.TopicResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubjectDto.SubjectResponse> listBySemester(Integer semester) {
        return subjectRepository.findBySemester(semester).stream()
                .map(s -> SubjectDto.SubjectResponse.from(s,
                        topicRepository.findBySubject(s).size(),
                        questionRepository.findByTopic_Subject_SubjectId(s.getSubjectId()).size()))
                .collect(Collectors.toList());
    }

    @Override
    public SubjectDto.PerformanceResponse getPerformance(Long studentId) {
        List<Subject> subjects = subjectRepository.findByActiveTrue();
        List<SubjectDto.SubjectPerformanceResponse> rows = new ArrayList<>();
        List<SubjectDto.QuestionPerformance> recent = new ArrayList<>();

        for (Subject s : subjects) {
            var attempts = attemptRepository.findByStudentAndSubject(studentId, s.getSubjectId());
            long total = attempts.size();
            long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
            BigDecimal accuracy = total == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgMarks = attempts.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getCorrect()) && a.getQuestion().getMarks() != null)
                    .map(a -> a.getQuestion().getMarks())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total > 0) {
                avgMarks = avgMarks.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
            }
            rows.add(new SubjectDto.SubjectPerformanceResponse(
                    s.getSubjectId(), s.getSubjectCode(), s.getSubjectName(),
                    total, correct, accuracy, avgMarks, null, null, null,
                    accuracy.compareTo(BigDecimal.valueOf(60)) < 0 ? "WEAK"
                            : accuracy.compareTo(BigDecimal.valueOf(80)) < 0 ? "AVERAGE" : "STRONG"));
        }

        List<com.student.performance.entity.QuestionAttempt> recentAttempts =
                attemptRepository.findRecentByStudent(studentId, LocalDateTime.now().minusDays(14));
        for (var a : recentAttempts) {
            var q = a.getQuestion();
            recent.add(new SubjectDto.QuestionPerformance(
                    a.getSelectedAnswer(), Boolean.TRUE.equals(a.getCorrect()),
                    q.getMarks(), a.getTimeTakenSec(), q.getQuestionText(),
                    q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                    q.getCorrectAnswer(), q.getExplanation(), q.getDifficulty()));
        }

        long totalAttempts = rows.stream().mapToLong(SubjectDto.SubjectPerformanceResponse::attempts).sum();
        long totalCorrect = rows.stream().mapToLong(SubjectDto.SubjectPerformanceResponse::correctAnswers).sum();
        BigDecimal overall = totalAttempts == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalCorrect * 100.0 / totalAttempts).setScale(2, RoundingMode.HALF_UP);
        return new SubjectDto.PerformanceResponse(rows, recent, overall);
    }

    @Override
    @Transactional
    public void saveSubjectMarks(List<SubjectDto.SubjectMarksRequest> requests, Long studentId) {
        Student student = securityUtils.getCurrentStudent();
        for (SubjectDto.SubjectMarksRequest req : requests) {
            Subject subject = subjectRepository.findById(req.subjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + req.subjectId()));
            BigDecimal weakness = computeWeakness(req);
            WeakSubject ws = weakSubjectRepository.findByStudentAndSubject_SubjectId(student, req.subjectId())
                    .orElseGet(() -> {
                        WeakSubject w = new WeakSubject();
                        w.setStudent(student);
                        w.setSubject(subject);
                        return w;
                    });
            ws.setWeaknessScore(weakness);
            ws.setAverageMarks(req.marks());
            ws.setAttendance(req.attendance());
            ws.setAssignmentScore(req.assignments());
            ws.setInternalMarks(req.internalMarks());
            ws.setPriorityRank(0);
            weakSubjectRepository.save(ws);
        }
    }

    @Override
    @Transactional
    public void updateStudentStreak(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        student.setStreakDays(student.getStreakDays() + 1);
        student.setXpPoints(student.getXpPoints() + 5);
        student.setLevel(student.getXpPoints() / 100 + 1);
        studentRepository.save(student);
    }

    private BigDecimal computeWeakness(SubjectDto.SubjectMarksRequest req) {
        double marks = req.marks() == null ? 0 : req.marks().doubleValue();
        double attendance = req.attendance() == null ? 100 : req.attendance().doubleValue();
        double assign = req.assignments() == null ? 0 : req.assignments().doubleValue();
        double internal = req.internalMarks() == null ? 0 : req.internalMarks().doubleValue();
        double score = marks * 0.4 + internal * 0.3 + assign * 0.2 + attendance * 0.1;
        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }
}
