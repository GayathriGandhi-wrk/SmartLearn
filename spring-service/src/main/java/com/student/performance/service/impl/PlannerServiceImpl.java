package com.student.performance.service.impl;

import com.student.performance.dto.PlannerDto;
import com.student.performance.entity.*;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.*;
import com.student.performance.service.PlannerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlannerServiceImpl implements PlannerService {

    private static final int MIN_SESSION_MINUTES = 30;
    private static final int MIN_ABSOLUTE_MINUTES = 15;

    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanSessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final StudentRepository studentRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final QuestionAttemptRepository questionAttemptRepository;
    private final PredictionRepository predictionRepository;
    private final WeakSubjectRepository weakSubjectRepository;

    public PlannerServiceImpl(StudyPlanRepository studyPlanRepository,
                              StudyPlanSessionRepository sessionRepository,
                              SubjectRepository subjectRepository,
                              TopicRepository topicRepository,
                              StudentRepository studentRepository,
                              KnowledgeGapRepository knowledgeGapRepository,
                              QuestionAttemptRepository questionAttemptRepository,
                              PredictionRepository predictionRepository,
                              WeakSubjectRepository weakSubjectRepository) {
        this.studyPlanRepository = studyPlanRepository;
        this.sessionRepository = sessionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.studentRepository = studentRepository;
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.questionAttemptRepository = questionAttemptRepository;
        this.predictionRepository = predictionRepository;
        this.weakSubjectRepository = weakSubjectRepository;
    }

    // =========================================================================
    // Generation
    // =========================================================================
    @Override
    @Transactional
    public PlannerDto.StudyPlanSummaryDto generatePlan(Long studentId, PlannerDto.StudyPlanRequest request) {
        Student student = requireStudent(studentId);
        RequestContext ctx = buildContext(student, request);
        return generate(student, request, ctx, null);
    }

    @Override
    @Transactional
    public PlannerDto.StudyPlanSummaryDto regeneratePlan(Long studentId, Long planId,
                                                         PlannerDto.StudyPlanRequest request) {
        Student student = requireStudent(studentId);
        StudyPlan existing = requireOwnedPlan(student, planId);
        RequestContext ctx = buildContext(student, request);
        return generate(student, request, ctx, existing);
    }

    private PlannerDto.StudyPlanSummaryDto generate(Student student, PlannerDto.StudyPlanRequest request,
                                                    RequestContext ctx, StudyPlan existing) {
        int days = ctx.days();
        int dayMinutes = ctx.dayMinutes();
        int totalMinutes = dayMinutes * days;
        int revisionMinutes = ctx.revisionDays() * dayMinutes;
        int studyMinutes = totalMinutes - revisionMinutes;

        List<SelectedTopic> deferred = new ArrayList<>();
        List<AllocationEntry> allocation = allocate(ctx.topics(), studyMinutes, deferred);

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(days - 1L);
        List<SessionSpec> specs = buildSessions(start, days, ctx.dailySlots(), allocation, ctx);

        StudyPlan plan;
        if (existing != null) {
            sessionRepository.deleteAll(sessionRepository.findByPlanOrderBySessionDateAscStartTimeAsc(existing));
            plan = existing;
        } else {
            plan = new StudyPlan();
        }
        plan.setStudent(student);
        plan.setTitle("Study Plan - " + ctx.type().name());
        plan.setPlanType(ctx.type());
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setTotalHours(Math.max(1, (int) Math.round(totalMinutes / 60.0)));
        plan.setStatus(StudyPlan.Status.ACTIVE);
        plan.setAiGenerated(true);
        plan = studyPlanRepository.save(plan);

        for (SessionSpec spec : specs) {
            StudyPlanSession session = new StudyPlanSession();
            session.setPlan(plan);
            session.setSessionDate(spec.date());
            session.setDay(spec.day());
            session.setStartTime(spec.start());
            session.setEndTime(spec.end());
            session.setDurationMinutes(spec.minutes());
            session.setStatus(StudyPlanSession.Status.PENDING);
            if (spec.topic() != null) {
                session.setSubject(spec.topic().subject());
                session.setTopic(spec.topic().topic());
                session.setDifficulty(spec.topic().difficulty());
                session.setPriority(spec.topic().priorityLabel());
                session.setActivity(spec.activity());
            } else {
                session.setPriority("MEDIUM");
                session.setActivity(spec.activity());
            }
            sessionRepository.save(session);
        }

        return toSummary(plan, ctx.topics(), deferred);
    }

    // =========================================================================
    // Queries
    // =========================================================================
    @Override
    public List<PlannerDto.StudyPlanSummaryDto> getPlans(Long studentId) {
        Student student = requireStudent(studentId);
        return studyPlanRepository.findByStudentOrderByStartDateDesc(student).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public PlannerDto.StudyPlanSummaryDto getPlan(Long studentId, Long planId) {
        Student student = requireStudent(studentId);
        return toSummary(requireOwnedPlan(student, planId));
    }

    @Override
    public PlannerDto.StudyPlanSummaryDto getReport(Long studentId, Long planId) {
        return getPlan(studentId, planId);
    }

    @Override
    @Transactional
    public void deletePlan(Long studentId, Long planId) {
        Student student = requireStudent(studentId);
        StudyPlan plan = requireOwnedPlan(student, planId);
        sessionRepository.deleteAll(sessionRepository.findByPlanOrderBySessionDateAscStartTimeAsc(plan));
        studyPlanRepository.delete(plan);
    }

    @Override
    @Transactional
    public PlannerDto.SessionDto completeSession(Long studentId, Long sessionId) {
        return setSessionStatus(studentId, sessionId, StudyPlanSession.Status.COMPLETED);
    }

    @Override
    @Transactional
    public PlannerDto.SessionDto skipSession(Long studentId, Long sessionId) {
        return setSessionStatus(studentId, sessionId, StudyPlanSession.Status.SKIPPED);
    }

    private PlannerDto.SessionDto setSessionStatus(Long studentId, Long sessionId, StudyPlanSession.Status status) {
        Student student = requireStudent(studentId);
        StudyPlanSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        if (!session.getPlan().getStudent().getStudentId().equals(student.getStudentId())) {
            throw new ResourceNotFoundException("Session not found");
        }
        session.setStatus(status);
        return toSessionDto(sessionRepository.save(session));
    }

    // =========================================================================
    // Request parsing + validation
    // =========================================================================
    private RequestContext buildContext(Student student, PlannerDto.StudyPlanRequest request) {
        if (request == null) {
            throw new BadRequestException("Planner request cannot be empty");
        }
        StudyPlan.PlanType type = parsePlanType(request.planType());
        SlotsInfo slotsInfo = buildSlots(request);

        if (request.subjects() == null || request.subjects().isEmpty()) {
            throw new BadRequestException("Select at least one subject");
        }

        double riskWeight = riskWeight(student);
        List<SelectedTopic> topics = new ArrayList<>();
        for (PlannerDto.SubjectSelection sel : request.subjects()) {
            if (sel.subjectId() == null) {
                throw new BadRequestException("A subject selection is missing its subject id");
            }
            Subject subject = subjectRepository.findById(sel.subjectId())
                    .orElseThrow(() -> new BadRequestException("Subject not found: " + sel.subjectId()));

            List<Topic> subjectTopics = topicRepository.findBySubjectSubjectId(subject.getSubjectId());
            if (subjectTopics.isEmpty()) {
                throw new BadRequestException("Subject '" + subject.getSubjectName() + "' has no topics");
            }

            List<Long> selectedIds = new ArrayList<>();
            if (sel.allTopics() || sel.topics() == null || sel.topics().isEmpty()) {
                selectedIds = subjectTopics.stream().map(Topic::getTopicId).collect(Collectors.toList());
            } else {
                for (Long id : sel.topics()) {
                    if (id == null) {
                        continue;
                    }
                    Topic t = topicRepository.findById(id)
                            .orElseThrow(() -> new BadRequestException("Topic not found: " + id));
                    if (t.getSubject() != null && t.getSubject().getSubjectId().equals(subject.getSubjectId())) {
                        selectedIds.add(id);
                    }
                }
            }
            if (selectedIds.isEmpty()) {
                throw new BadRequestException("Select at least one topic for subject '" + subject.getSubjectName() + "'");
            }

            double subjectWeight = subjectWeight(student, subject);
            for (Long topicId : selectedIds) {
                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new BadRequestException("Topic not found: " + topicId));
                Double perf = topicPerformance(student, topic);
                double score = difficultyWeight(topic.getDifficultyLevel()) + weaknessWeight(perf)
                        + riskWeight + subjectWeight;
                String label = priorityLabel(score);
                topics.add(new SelectedTopic(subject, topic, score, perf, label,
                        topic.getDifficultyLevel(), activityFor(label, perf), reasonFor(perf, topic)));
            }
        }

        int revisionDays = type == StudyPlan.PlanType.DAILY ? 0 : type == StudyPlan.PlanType.MONTHLY ? 4 : 1;
        return new RequestContext(type, daysFor(type), slotsInfo.dayMinutes(), slotsInfo.slots(), topics, revisionDays);
    }

    private StudyPlan.PlanType parsePlanType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Select a plan type (Daily, Weekly or Monthly)");
        }
        try {
            return StudyPlan.PlanType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid plan type: " + raw);
        }
    }

    private SlotsInfo buildSlots(PlannerDto.StudyPlanRequest request) {
        List<Slot> slots = new ArrayList<>();
        PlannerDto.AvailableTime at = request.availableTime();
        if (at != null) {
            addSlot(slots, "Morning", at.morning());
            addSlot(slots, "Afternoon", at.afternoon());
            addSlot(slots, "Evening", at.evening());
            addSlot(slots, "Night", at.night());
        }
        int slotsMinutes = slots.stream().mapToInt(Slot::minutes).sum();
        int dayMinutes = request.totalHoursPerDay() != null && request.totalHoursPerDay() > 0
                ? request.totalHoursPerDay() * 60 : slotsMinutes;

        if (slots.isEmpty() && dayMinutes <= 0) {
            throw new BadRequestException("No available study time selected");
        }
        if (dayMinutes <= 0) {
            throw new BadRequestException("Available study time must be greater than zero");
        }
        if (dayMinutes > 24 * 60) {
            throw new BadRequestException("Available study time per day cannot exceed 24 hours");
        }

        if (slots.isEmpty()) {
            LocalTime start = LocalTime.of(6, 0);
            slots.add(new Slot(start, start.plusMinutes(dayMinutes), dayMinutes));
            return new SlotsInfo(slots, dayMinutes);
        }

        int diff = dayMinutes - slotsMinutes;
        Slot last = slots.get(slots.size() - 1);
        int newMinutes = last.minutes() + diff;
        if (newMinutes < MIN_ABSOLUTE_MINUTES) {
            slots.remove(slots.size() - 1);
            diff += last.minutes();
            if (slots.isEmpty()) {
                throw new BadRequestException("No available study time selected");
            }
            last = slots.get(slots.size() - 1);
            newMinutes = last.minutes() + diff;
        }
        slots.set(slots.size() - 1, new Slot(last.start(), last.start().plusMinutes(newMinutes), newMinutes));
        return new SlotsInfo(slots, dayMinutes);
    }

    private void addSlot(List<Slot> slots, String name, PlannerDto.TimeSlot ts) {
        if (ts == null || !ts.enabled()) {
            return;
        }
        LocalTime start = parseTime(ts.start());
        LocalTime end = parseTime(ts.end());
        if (start == null || end == null) {
            throw new BadRequestException(name + " slot is missing a start or end time");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException(name + " slot end time must be after its start time");
        }
        slots.add(new Slot(start, end, (int) Duration.between(start, end).toMinutes()));
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid time format: " + value + " (use HH:mm)");
        }
    }

    // =========================================================================
    // Performance / priority
    // =========================================================================
    private Double topicPerformance(Student student, Topic topic) {
        Optional<KnowledgeGap> gap = knowledgeGapRepository.findByStudentAndTopic_TopicId(student, topic.getTopicId());
        if (gap.isPresent() && gap.get().getMasteryLevel() != null) {
            return gap.get().getMasteryLevel().doubleValue();
        }
        List<QuestionAttempt> attempts =
                questionAttemptRepository.findByStudentAndTopic(student.getStudentId(), topic.getTopicId());
        if (!attempts.isEmpty()) {
            long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
            return correct * 100.0 / attempts.size();
        }
        return null;
    }

    private double riskWeight(Student student) {
        Optional<Prediction> prediction = predictionRepository.findFirstByStudentOrderByCreatedAtDesc(student);
        if (prediction.isEmpty() || prediction.get().getRiskLevel() == null) {
            return 0.5;
        }
        return switch (prediction.get().getRiskLevel()) {
            case LOW -> 0.0;
            case MEDIUM -> 0.5;
            case HIGH -> 1.0;
            case CRITICAL -> 1.5;
        };
    }

    private double subjectWeight(Student student, Subject subject) {
        Optional<WeakSubject> ws = weakSubjectRepository.findByStudentAndSubject_SubjectId(student, subject.getSubjectId());
        if (ws.isEmpty() || ws.get().getAverageMarks() == null) {
            return 0.5;
        }
        return Math.max(0.0, (100 - ws.get().getAverageMarks().doubleValue()) / 100.0);
    }

    private double difficultyWeight(String difficulty) {
        return switch (difficulty == null ? "" : difficulty.toUpperCase()) {
            case "BEGINNER" -> 1.0;
            case "INTERMEDIATE" -> 1.5;
            case "ADVANCED" -> 2.0;
            default -> 1.0;
        };
    }

    private double weaknessWeight(Double perf) {
        if (perf == null) {
            return 1.0;
        }
        return (100 - perf) / 100.0 * 2.0;
    }

    private String priorityLabel(double score) {
        if (score >= 3.5) {
            return "CRITICAL";
        }
        if (score >= 2.5) {
            return "HIGH";
        }
        if (score >= 1.5) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String activityFor(String label, Double perf) {
        if ((perf != null && perf < 50) || "HIGH".equals(label) || "CRITICAL".equals(label)) {
            return "Learn Concept + Practice MCQs";
        }
        if ("MEDIUM".equals(label)) {
            return "Revise + Practice Questions";
        }
        return "Quick Revision + Advanced Questions";
    }

    private String reasonFor(Double perf, Topic topic) {
        if (perf != null) {
            return "Performance in this topic is " + Math.round(perf) + "% — " + priorityLabel(
                    2.0 + (100 - perf) / 100.0 * 2.0);
        }
        return "No performance data — allocated by difficulty (" + topic.getDifficultyLevel() + ")";
    }

    // =========================================================================
    // Allocation
    // =========================================================================
    private List<AllocationEntry> allocate(List<SelectedTopic> topics, int studyMinutes,
                                           List<SelectedTopic> deferredOut) {
        if (studyMinutes < MIN_ABSOLUTE_MINUTES) {
            throw new BadRequestException("Available study time is too little to create a plan "
                    + "(need at least " + MIN_ABSOLUTE_MINUTES + " minutes)");
        }
        List<SelectedTopic> sorted = topics.stream()
                .sorted(Comparator.comparingDouble(SelectedTopic::priorityScore).reversed())
                .collect(Collectors.toList());

        int s = studyMinutes;
        int min = Math.min(MIN_SESSION_MINUTES, Math.max(MIN_ABSOLUTE_MINUTES, s));
        List<SelectedTopic> remaining = new ArrayList<>(sorted);
        while (remaining.size() * min > s && remaining.size() > 1) {
            deferredOut.add(remaining.remove(remaining.size() - 1));
        }

        int k = remaining.size();
        if (k == 0) {
            return List.of();
        }
        int guaranteed = min * k;
        int left = s - guaranteed;
        double totalScore = remaining.stream().mapToDouble(SelectedTopic::priorityScore).sum();
        double totalScoreSafe = totalScore <= 0 ? 1.0 : totalScore;

        int[] minutes = new int[k];
        double[] frac = new double[k];
        int allocated = 0;
        for (int i = 0; i < k; i++) {
            minutes[i] = guaranteed / k;
            double raw = left * (remaining.get(i).priorityScore() / totalScoreSafe);
            frac[i] = raw;
            minutes[i] += (int) Math.floor(raw);
            allocated += (int) Math.floor(raw);
        }
        Integer[] order = new Integer[k];
        for (int i = 0; i < k; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(frac[b] - Math.floor(frac[b]), frac[a] - Math.floor(frac[a])));
        int idx = 0;
        while (allocated < left && idx < k) {
            minutes[order[idx]]++;
            allocated++;
            idx++;
        }

        List<AllocationEntry> out = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            out.add(new AllocationEntry(remaining.get(i), minutes[i]));
        }
        return out;
    }

    // =========================================================================
    // Session scheduling
    // =========================================================================
    private List<SessionSpec> buildSessions(LocalDate start, int days, List<Slot> dailySlots,
                                            List<AllocationEntry> allocation, RequestContext ctx) {
        List<SessionSpec> out = new ArrayList<>();
        int studyDays = Math.max(1, days - ctx.revisionDays());

        List<AllocationEntry> queue = new ArrayList<>(allocation);
        int cursor = 0;
        outer:
        for (int d = 0; d < studyDays; d++) {
            LocalDate date = start.plusDays(d);
            String dayName = titleCase(date.getDayOfWeek().toString());
            for (Slot slot : dailySlots) {
                int remaining = slot.minutes();
                LocalTime t = slot.start();
                while (remaining > 0) {
                    while (cursor < queue.size() && queue.get(cursor).minutes() <= 0) {
                        cursor++;
                    }
                    if (cursor >= queue.size()) {
                        break outer;
                    }
                    AllocationEntry e = queue.get(cursor);
                    int take = Math.min(e.minutes(), remaining);
                    LocalTime end = t.plusMinutes(take);
                    out.add(new SessionSpec(date, dayName, t, end, take, e.topic(),
                            activityFor(e.topic().priorityLabel(), e.topic().performance())));
                    queue.set(cursor, new AllocationEntry(e.topic(), e.minutes() - take));
                    remaining -= take;
                    t = end;
                }
            }
        }

        for (int d = studyDays; d < days; d++) {
            LocalDate date = start.plusDays(d);
            String dayName = titleCase(date.getDayOfWeek().toString());
            for (Slot slot : dailySlots) {
                boolean last = d == days - 1;
                out.add(new SessionSpec(date, dayName, slot.start(), slot.end(), slot.minutes(), null,
                        last ? "Mock Test + Weak Topic Revision" : "Revision + Practice"));
            }
        }
        return out;
    }

    private String titleCase(String name) {
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private int daysFor(StudyPlan.PlanType type) {
        return switch (type) {
            case DAILY -> 1;
            case WEEKLY -> 7;
            case MONTHLY -> 30;
            case EXAM -> 7;
        };
    }

    // =========================================================================
    // DTO mapping
    // =========================================================================
    private PlannerDto.StudyPlanSummaryDto toSummary(StudyPlan plan, List<SelectedTopic> plannedTopics,
                                                     List<SelectedTopic> deferred) {
        List<StudyPlanSession> sessions = sessionRepository.findByPlanOrderBySessionDateAscStartTimeAsc(plan);
        List<PlannerDto.SessionDto> sessionDtos = sessions.stream().map(this::toSessionDto).collect(Collectors.toList());

        Map<String, Integer> subjectMinutes = new LinkedHashMap<>();
        Map<String, Integer> topicMinutes = new LinkedHashMap<>();
        int completed = 0;
        int highPriority = 0;
        for (StudyPlanSession s : sessions) {
            String subj = s.getSubject() != null ? s.getSubject().getSubjectName() : "Revision";
            String top = s.getTopic() != null ? s.getTopic().getTopicName() : "Revision";
            subjectMinutes.merge(subj, s.getDurationMinutes(), Integer::sum);
            topicMinutes.merge(top, s.getDurationMinutes(), Integer::sum);
            if (s.getStatus() == StudyPlanSession.Status.COMPLETED) {
                completed++;
            }
            if (s.getPriority() != null && (s.getPriority().equals("HIGH") || s.getPriority().equals("CRITICAL"))) {
                highPriority++;
            }
        }
        int totalMinutes = sessions.stream().mapToInt(StudyPlanSession::getDurationMinutes).sum();
        int sessionCount = sessions.size();
        int completionPercent = sessionCount == 0 ? 0 : (int) Math.round(completed * 100.0 / sessionCount);

        List<PlannerDto.PriorityTopicDto> priorityTopics = new ArrayList<>();
        for (SelectedTopic t : plannedTopics) {
            double hours = round1(topicMinutes.getOrDefault(t.topic().getTopicName(), 0) / 60.0);
            priorityTopics.add(new PlannerDto.PriorityTopicDto(
                    t.subject().getSubjectName(), t.topic().getTopicName(), t.priorityLabel(),
                    t.difficulty(), hours, t.reason(), t.activity()));
        }
        List<PlannerDto.PriorityTopicDto> deferredTopics = new ArrayList<>();
        for (SelectedTopic t : deferred) {
            deferredTopics.add(new PlannerDto.PriorityTopicDto(
                    t.subject().getSubjectName(), t.topic().getTopicName(), t.priorityLabel(),
                    t.difficulty(), 0.0, "Deferred — not enough available time", t.activity()));
        }

        return new PlannerDto.StudyPlanSummaryDto(
                plan.getPlanId(), plan.getPlanType().name(), plan.getStartDate(), plan.getEndDate(),
                totalMinutes, round1(totalMinutes / 60.0),
                subjectMinutes.size(), topicMinutes.size(), sessionCount, highPriority,
                completed, completionPercent, sessionDtos,
                toDistribution(subjectMinutes), toDistribution(topicMinutes),
                priorityTopics, deferredTopics);
    }

    private PlannerDto.StudyPlanSummaryDto toSummary(StudyPlan plan) {
        List<StudyPlanSession> sessions = sessionRepository.findByPlanOrderBySessionDateAscStartTimeAsc(plan);
        List<PlannerDto.SessionDto> sessionDtos = sessions.stream().map(this::toSessionDto).collect(Collectors.toList());

        Map<String, Integer> subjectMinutes = new LinkedHashMap<>();
        Map<String, Integer> topicMinutes = new LinkedHashMap<>();
        Map<String, PlannerDto.PriorityTopicDto> byTopic = new LinkedHashMap<>();
        int completed = 0;
        int highPriority = 0;
        for (StudyPlanSession s : sessions) {
            String subj = s.getSubject() != null ? s.getSubject().getSubjectName() : "Revision";
            String top = s.getTopic() != null ? s.getTopic().getTopicName() : "Revision";
            subjectMinutes.merge(subj, s.getDurationMinutes(), Integer::sum);
            topicMinutes.merge(top, s.getDurationMinutes(), Integer::sum);
            if (s.getTopic() != null && s.getSubject() != null) {
                byTopic.merge(top,
                        new PlannerDto.PriorityTopicDto(subj, top,
                                s.getPriority() == null ? "LOW" : s.getPriority(),
                                s.getDifficulty() == null ? "" : s.getDifficulty(),
                                s.getDurationMinutes() / 60.0, "Priority topic for improvement",
                                s.getActivity() == null ? "" : s.getActivity()),
                        (a, b) -> new PlannerDto.PriorityTopicDto(a.subject(), a.topic(),
                                higherPriority(a.priority(), b.priority()), a.difficulty(),
                                a.recommendedHours() + b.recommendedHours(), a.reason(), a.activity()));
            }
            if (s.getStatus() == StudyPlanSession.Status.COMPLETED) {
                completed++;
            }
            if (s.getPriority() != null && (s.getPriority().equals("HIGH") || s.getPriority().equals("CRITICAL"))) {
                highPriority++;
            }
        }
        int totalMinutes = sessions.stream().mapToInt(StudyPlanSession::getDurationMinutes).sum();
        int sessionCount = sessions.size();
        int completionPercent = sessionCount == 0 ? 0 : (int) Math.round(completed * 100.0 / sessionCount);
        List<PlannerDto.PriorityTopicDto> priorityTopics = new ArrayList<>(byTopic.values());

        return new PlannerDto.StudyPlanSummaryDto(
                plan.getPlanId(), plan.getPlanType().name(), plan.getStartDate(), plan.getEndDate(),
                totalMinutes, round1(totalMinutes / 60.0),
                subjectMinutes.size(), topicMinutes.size(), sessionCount, highPriority,
                completed, completionPercent, sessionDtos,
                toDistribution(subjectMinutes), toDistribution(topicMinutes),
                priorityTopics, List.of());
    }

    private String higherPriority(String a, String b) {
        return priorityRank(a) >= priorityRank(b) ? a : b;
    }

    private int priorityRank(String p) {
        return switch (p == null ? "" : p) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private List<PlannerDto.DistributionEntry> toDistribution(Map<String, Integer> minutes) {
        return minutes.entrySet().stream()
                .map(e -> new PlannerDto.DistributionEntry(e.getKey(), round1(e.getValue() / 60.0)))
                .collect(Collectors.toList());
    }

    private PlannerDto.SessionDto toSessionDto(StudyPlanSession s) {
        return new PlannerDto.SessionDto(
                s.getSessionId(), s.getSessionDate(), s.getDay(),
                s.getStartTime() == null ? "" : s.getStartTime().toString(),
                s.getEndTime() == null ? "" : s.getEndTime().toString(),
                s.getDurationMinutes(),
                s.getSubject() == null ? null : s.getSubject().getSubjectName(),
                s.getTopic() == null ? null : s.getTopic().getTopicName(),
                s.getDifficulty(), s.getPriority(), s.getActivity(),
                s.getStatus().name());
    }

    private Student requireStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
    }

    private StudyPlan requireOwnedPlan(Student student, Long planId) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found: " + planId));
        if (!plan.getStudent().getStudentId().equals(student.getStudentId())) {
            throw new ResourceNotFoundException("Study plan not found");
        }
        return plan;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // =========================================================================
    // Internal models
    // =========================================================================
    private record Slot(LocalTime start, LocalTime end, int minutes) {}

    private record SlotsInfo(List<Slot> slots, int dayMinutes) {}

    private record SelectedTopic(Subject subject, Topic topic, double priorityScore, Double performance,
                                 String priorityLabel, String difficulty, String activity, String reason) {}

    private record AllocationEntry(SelectedTopic topic, int minutes) {}

    private record SessionSpec(LocalDate date, String day, LocalTime start, LocalTime end, int minutes,
                               SelectedTopic topic, String activity) {}

    private record RequestContext(StudyPlan.PlanType type, int days, int dayMinutes,
                                  List<Slot> dailySlots, List<SelectedTopic> topics, int revisionDays) {}
}
