package com.student.performance.service.impl;

import com.student.performance.dto.AnalyticsDto;
import com.student.performance.entity.QuestionAttempt;
import com.student.performance.entity.Student;
import com.student.performance.entity.StudyPlan;
import com.student.performance.entity.StudyPlanSession;
import com.student.performance.entity.TestResult;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.QuestionAttemptRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.StudyPlanRepository;
import com.student.performance.repository.StudyPlanSessionRepository;
import com.student.performance.repository.TestResultRepository;
import com.student.performance.service.AnalyticsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TestResultRepository testResultRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final StudentRepository studentRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanSessionRepository sessionRepository;

    public AnalyticsServiceImpl(TestResultRepository testResultRepository,
                                QuestionAttemptRepository attemptRepository,
                                StudentRepository studentRepository,
                                StudyPlanRepository studyPlanRepository,
                                StudyPlanSessionRepository sessionRepository) {
        this.testResultRepository = testResultRepository;
        this.attemptRepository = attemptRepository;
        this.studentRepository = studentRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public AnalyticsDto.AnalyticsResponse getAnalytics(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);
        List<TestResult> weekly = testResultRepository.findByStudentSince(studentId, weekAgo);
        List<TestResult> monthly = testResultRepository.findByStudentSince(studentId, monthAgo);

        List<AnalyticsDto.TrendPoint> weeklyTrend = buildTrend(weekly, 7);
        List<AnalyticsDto.TrendPoint> monthlyTrend = buildTrend(monthly, 30);

        Map<String, List<QuestionAttempt>> bySubject = new HashMap<>();
        for (QuestionAttempt a : attemptRepository.findByStudent(student)) {
            String sub = a.getQuestion().getTopic().getSubject().getSubjectName();
            bySubject.computeIfAbsent(sub, k -> new ArrayList<>()).add(a);
        }
        List<AnalyticsDto.SubjectRadarPoint> radar = bySubject.entrySet().stream()
                .map(e -> {
                    long correct = e.getValue().stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
                    double acc = e.getValue().isEmpty() ? 0 : correct * 100.0 / e.getValue().size();
                    return new AnalyticsDto.SubjectRadarPoint(e.getKey(), Math.round(acc * 100.0) / 100.0);
                })
                .toList();

        Map<String, long[]> diffMap = new LinkedHashMap<>();
        for (QuestionAttempt a : attemptRepository.findByStudent(student)) {
            String d = a.getQuestion().getDifficulty().name();
            long[] arr = diffMap.computeIfAbsent(d, k -> new long[2]);
            if (Boolean.TRUE.equals(a.getCorrect())) {
                arr[0]++;
            } else {
                arr[1]++;
            }
        }
        List<AnalyticsDto.DistributionPoint> distribution = diffMap.entrySet().stream()
                .map(e -> new AnalyticsDto.DistributionPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        Map<String, Long> activity = new LinkedHashMap<>();
        for (QuestionAttempt a : attemptRepository.findByStudent(student)) {
            String key = switch (a.getAttemptedAt().getDayOfWeek()) {
                case MONDAY -> "Mon";
                case TUESDAY -> "Tue";
                case WEDNESDAY -> "Wed";
                case THURSDAY -> "Thu";
                case FRIDAY -> "Fri";
                case SATURDAY -> "Sat";
                case SUNDAY -> "Sun";
            };
            activity.merge(key, 1L, Long::sum);
        }

        Double avg = testResultRepository.averagePercentage(studentId);
        long totalAnswered = attemptRepository.countByStudent_StudentId(studentId);
        long totalTests = testResultRepository.findByStudent_StudentIdOrderByCreatedAtDesc(studentId).size();

        AnalyticsDto.StudyPlanProgress studyPlanProgress = buildStudyPlanProgress(student, studentId);

        return new AnalyticsDto.AnalyticsResponse(
                weeklyTrend, monthlyTrend, radar, distribution, activity,
                totalTests, totalAnswered,
                avg == null ? BigDecimal.ZERO : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP),
                student.getStreakDays(), student.getLevel(), student.getXpPoints(),
                studyPlanProgress);
    }

    private AnalyticsDto.StudyPlanProgress buildStudyPlanProgress(Student student, Long studentId) {
        List<StudyPlan> activePlans = studyPlanRepository.findByStudentAndStatus(student, StudyPlan.Status.ACTIVE);
        List<StudyPlanSession> sessions =
                sessionRepository.findByPlan_Student_StudentIdOrderBySessionDateAscStartTimeAsc(studentId);

        long total = sessions.size();
        long completed = sessions.stream()
                .filter(s -> s.getStatus() == StudyPlanSession.Status.COMPLETED).count();
        long skipped = sessions.stream()
                .filter(s -> s.getStatus() == StudyPlanSession.Status.SKIPPED).count();
        long pending = total - completed - skipped;
        int completionPercent = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
        double hoursStudied = round1(sessions.stream()
                .filter(s -> s.getStatus() == StudyPlanSession.Status.COMPLETED)
                .mapToInt(StudyPlanSession::getDurationMinutes).sum() / 60.0);

        Map<LocalDate, int[]> daily = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            daily.put(today.minusDays(i), new int[3]);
        }
        for (StudyPlanSession s : sessions) {
            int[] arr = daily.get(s.getSessionDate());
            if (arr == null) {
                continue;
            }
            arr[0] += s.getDurationMinutes();
            if (s.getStatus() == StudyPlanSession.Status.COMPLETED) {
                arr[1] += s.getDurationMinutes();
            } else if (s.getStatus() == StudyPlanSession.Status.SKIPPED) {
                arr[2] += s.getDurationMinutes();
            }
        }
        List<AnalyticsDto.DailyStudyPoint> dailyProgress = daily.entrySet().stream()
                .map(e -> new AnalyticsDto.DailyStudyPoint(
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();

        Map<String, long[]> bySubject = new LinkedHashMap<>();
        for (StudyPlanSession s : sessions) {
            if (s.getStatus() != StudyPlanSession.Status.COMPLETED) {
                continue;
            }
            String subj = s.getSubject() != null ? s.getSubject().getSubjectName() : "Revision";
            long[] arr = bySubject.computeIfAbsent(subj, k -> new long[2]);
            arr[0] += s.getDurationMinutes();
            arr[1]++;
        }
        List<AnalyticsDto.SubjectStudyHours> subjectHours = bySubject.entrySet().stream()
                .map(e -> new AnalyticsDto.SubjectStudyHours(
                        e.getKey(), round1(e.getValue()[0] / 60.0), e.getValue()[1]))
                .toList();

        return new AnalyticsDto.StudyPlanProgress(
                activePlans.size(), total, completed, skipped, pending,
                completionPercent, hoursStudied, dailyProgress, subjectHours);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<AnalyticsDto.TrendPoint> buildTrend(List<TestResult> results, int days) {
        Map<LocalDate, BigDecimal> map = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            map.put(today.minusDays(i), null);
        }
        for (TestResult r : results) {
            LocalDate date = r.getCreatedAt().toLocalDate();
            map.put(date, r.getPercentage());
        }
        List<AnalyticsDto.TrendPoint> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : map.entrySet()) {
            trend.add(new AnalyticsDto.TrendPoint(e.getKey(), e.getValue()));
        }
        return trend;
    }
}
