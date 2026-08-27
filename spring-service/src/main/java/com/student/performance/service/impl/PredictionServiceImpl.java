package com.student.performance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.performance.dto.PredictionDto;
import com.student.performance.entity.KnowledgeGap;
import com.student.performance.entity.Prediction;
import com.student.performance.entity.Question;
import com.student.performance.entity.QuestionAttempt;
import com.student.performance.entity.Student;
import com.student.performance.entity.Subject;
import com.student.performance.entity.Test;
import com.student.performance.entity.TestQuestion;
import com.student.performance.entity.Topic;
import com.student.performance.entity.WeakSubject;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.KnowledgeGapRepository;
import com.student.performance.repository.PredictionRepository;
import com.student.performance.repository.QuestionAttemptRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.SubjectRepository;
import com.student.performance.repository.TestQuestionRepository;
import com.student.performance.repository.TestRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.repository.WeakSubjectRepository;
import com.student.performance.service.AiServiceClient;
import com.student.performance.service.PredictionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionServiceImpl implements PredictionService {

    private final PredictionRepository predictionRepository;
    private final WeakSubjectRepository weakSubjectRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PredictionServiceImpl(PredictionRepository predictionRepository,
                                 WeakSubjectRepository weakSubjectRepository,
                                 KnowledgeGapRepository knowledgeGapRepository,
                                 QuestionAttemptRepository attemptRepository,
                                 StudentRepository studentRepository,
                                 SubjectRepository subjectRepository,
                                 TopicRepository topicRepository,
                                 QuestionRepository questionRepository,
                                 TestRepository testRepository,
                                 TestQuestionRepository testQuestionRepository,
                                 AiServiceClient aiServiceClient) {
        this.predictionRepository = predictionRepository;
        this.weakSubjectRepository = weakSubjectRepository;
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.attemptRepository = attemptRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    @Transactional
    public PredictionDto.PredictionResponse predict(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        Map<String, Object> features = buildFeatures(student);
        Map<String, Object> result = aiServiceClient.predict(features);

        boolean aiAvailable = !Boolean.FALSE.equals(result.get("available"));
        BigDecimal score = aiAvailable && result.get("predicted_score") != null
                ? new BigDecimal(result.get("predicted_score").toString())
                : fallbackScore(student);
        String grade = aiAvailable && result.get("predicted_grade") != null
                ? result.get("predicted_grade").toString()
                : gradeFor(score);
        String risk = aiAvailable && result.get("risk_level") != null
                ? result.get("risk_level").toString()
                : riskFor(score);
        BigDecimal confidence = aiAvailable && result.get("confidence_score") != null
                ? new BigDecimal(result.get("confidence_score").toString())
                : BigDecimal.valueOf(85);

        Map<String, Double> importance = new LinkedHashMap<>();
        if (aiAvailable && result.get("feature_importance") instanceof Map<?, ?> fi) {
            fi.forEach((k, v) -> importance.put(String.valueOf(k), ((Number) v).doubleValue()));
        } else {
            importance.putAll(buildLocalImportance(student));
        }

        String explanation = aiAvailable && result.get("explanation") != null
                ? result.get("explanation").toString()
                : buildLocalExplanation(score, risk);

        Prediction prediction = new Prediction();
        prediction.setStudent(student);
        prediction.setModelName(aiAvailable && result.get("model_name") != null
                ? result.get("model_name").toString() : "RuleBased");
        prediction.setPredictedGrade(grade);
        prediction.setPredictedScore(score);
        prediction.setRiskLevel(Prediction.RiskLevel.valueOf(risk.toUpperCase()));
        prediction.setConfidenceScore(confidence);
        try {
            prediction.setFeatureImportance(objectMapper.writeValueAsString(importance));
            prediction.setInputFeatures(objectMapper.writeValueAsString(features));
        } catch (Exception ignored) {
        }
        prediction.setExplanation(explanation);
        prediction = predictionRepository.save(prediction);

        List<PredictionDto.WeakSubjectDto> weakSubjects = detectWeakSubjects(studentId);
        List<PredictionDto.KnowledgeGapDto> gaps = detectKnowledgeGaps(studentId);

        return toResponse(prediction, weakSubjects, gaps);
    }

    @Override
    public PredictionDto.PredictionResponse getLatestPrediction(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Prediction prediction = predictionRepository.findFirstByStudentOrderByCreatedAtDesc(student)
                .orElseThrow(() -> new ResourceNotFoundException("No prediction found. Generate one first."));
        return toResponse(prediction, detectWeakSubjects(studentId), detectKnowledgeGaps(studentId));
    }

    @Override
    @Transactional
    public List<PredictionDto.WeakSubjectDto> detectWeakSubjects(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        weakSubjectRepository.deleteByStudent(student);

        List<Subject> subjects = subjectRepository.findByActiveTrue();
        List<WeakSubject> result = new ArrayList<>();
        for (Subject s : subjects) {
            List<QuestionAttempt> attempts = attemptRepository.findByStudentAndSubject(studentId, s.getSubjectId());
            if (attempts.isEmpty()) {
                continue;
            }
            long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
            double accuracy = correct * 100.0 / attempts.size();
            double weakness = 100 - accuracy;
            WeakSubject ws = new WeakSubject();
            ws.setStudent(student);
            ws.setSubject(s);
            ws.setWeaknessScore(BigDecimal.valueOf(weakness).setScale(2, RoundingMode.HALF_UP));
            ws.setAverageMarks(BigDecimal.valueOf(accuracy).setScale(2, RoundingMode.HALF_UP));
            ws.setPriorityRank(0);
            result.add(ws);
        }
        result.sort((a, b) -> b.getWeaknessScore().compareTo(a.getWeaknessScore()));
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setPriorityRank(i + 1);
        }
        weakSubjectRepository.saveAll(result);

        return result.stream().limit(5)
                .map(w -> new PredictionDto.WeakSubjectDto(
                        w.getSubject().getSubjectId(), w.getSubject().getSubjectCode(),
                        w.getSubject().getSubjectName(), w.getWeaknessScore(),
                        BigDecimal.valueOf(100).subtract(w.getWeaknessScore()), w.getPriorityRank()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PredictionDto.KnowledgeGapDto> detectKnowledgeGaps(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        knowledgeGapRepository.deleteByStudent(student);

        List<Topic> topics = topicRepository.findAll();
        List<KnowledgeGap> result = new ArrayList<>();
        for (Topic t : topics) {
            List<QuestionAttempt> attempts = attemptRepository.findByStudentAndTopic(studentId, t.getTopicId());
            if (attempts.isEmpty()) {
                continue;
            }
            long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
            double accuracy = correct * 100.0 / attempts.size();
            double mastery = accuracy;
            KnowledgeGap gap = new KnowledgeGap();
            gap.setStudent(student);
            gap.setTopic(t);
            gap.setMasteryLevel(BigDecimal.valueOf(mastery).setScale(2, RoundingMode.HALF_UP));
            gap.setGapLevel(gapLevelFor(accuracy));
            gap.setAttemptedQuestions(attempts.size());
            gap.setCorrectQuestions((int) correct);
            gap.setRecommendedHours(Math.max(1, (int) Math.ceil((100 - mastery) / 20)));
            result.add(gap);
        }
        result.sort((a, b) -> a.getMasteryLevel().compareTo(b.getMasteryLevel()));
        knowledgeGapRepository.saveAll(result);

        return result.stream().limit(10)
                .map(g -> new PredictionDto.KnowledgeGapDto(
                        g.getTopic().getTopicId(), g.getTopic().getTopicName(),
                        g.getTopic().getSubject().getSubjectName(), g.getMasteryLevel(),
                        g.getGapLevel().name(), g.getAttemptedQuestions(), g.getCorrectQuestions(),
                        g.getRecommendedHours()))
                .collect(Collectors.toList());
    }

    @Override
    public PredictionDto.PerformanceFactorsResponse performanceFactors(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        List<QuestionAttempt> attempts = attemptRepository.findByStudent(student);
        long total = attempts.size();
        long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        double accuracy = total == 0 ? 0 : correct * 100.0 / total;

        double easy = accuracyFor(attempts, Question.Difficulty.BEGINNER);
        double medium = accuracyFor(attempts, Question.Difficulty.INTERMEDIATE);
        double hard = accuracyFor(attempts, Question.Difficulty.ADVANCED);

        long activeQuestions = questionRepository.countActive();
        double attemptedPct = activeQuestions == 0 ? 0 : Math.min(100, total * 100.0 / activeQuestions);

        List<Test> completed = testRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(t -> t.getStatus() == Test.Status.COMPLETED && t.getPercentage() != null)
                .toList();
        double avgTestScore = completed.stream()
                .mapToDouble(t -> t.getPercentage().doubleValue())
                .average().orElse(0);

        double recentImprovement = 0;
        if (completed.size() >= 2) {
            double latest = completed.get(0).getPercentage().doubleValue();
            double previousAvg = completed.stream().skip(1)
                    .mapToDouble(t -> t.getPercentage().doubleValue())
                    .average().orElse(latest);
            recentImprovement = latest - previousAvg;
        }

        String consistency = student.getStreakDays() >= 7 ? "Good"
                : student.getStreakDays() >= 3 ? "Average" : "Poor";

        List<PredictionDto.PerformanceFactorDto> factors = new ArrayList<>();
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Previous test scores", pct(avgTestScore), statusFor(avgTestScore)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Accuracy", pct(accuracy), statusFor(accuracy)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Questions attempted", pct(attemptedPct), statusFor(attemptedPct)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Easy questions", pct(easy), statusFor(easy)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Medium questions", pct(medium), statusFor(medium)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Hard questions", pct(hard), statusFor(hard)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Recent improvement", signPct(recentImprovement), statusForImprovement(recentImprovement)));
        factors.add(new PredictionDto.PerformanceFactorDto(
                "Study consistency", consistency, consistencyStatus(consistency)));

        String summary = "These factors are computed from your tests, practice attempts and study activity, "
                + "and are used by the model to arrive at your predicted score.";
        return new PredictionDto.PerformanceFactorsResponse(factors, summary);
    }

    @Override
    public PredictionDto.TopicPredictionResponse topicPrediction(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        List<PredictionDto.TopicScoreDto> topics = new ArrayList<>();
        for (Topic t : topicRepository.findAll()) {
            List<QuestionAttempt> attempts = attemptRepository.findByStudentAndTopic(studentId, t.getTopicId());
            if (attempts.isEmpty()) {
                continue;
            }
            long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
            double score = correct * 100.0 / attempts.size();
            String category = score >= 75 ? "STRONG" : score >= 50 ? "AVERAGE" : "WEAK";
            topics.add(new PredictionDto.TopicScoreDto(
                    t.getTopicId(), t.getTopicName(), t.getSubject().getSubjectName(),
                    BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                    attempts.size(), (int) correct, category));
        }
        topics.sort((a, b) -> b.score().compareTo(a.score()));

        List<PredictionDto.TopicScoreDto> strong = topics.stream()
                .filter(t -> "STRONG".equals(t.category())).toList();
        List<PredictionDto.TopicScoreDto> average = topics.stream()
                .filter(t -> "AVERAGE".equals(t.category())).toList();
        List<PredictionDto.TopicScoreDto> weak = topics.stream()
                .filter(t -> "WEAK".equals(t.category())).toList();

        return new PredictionDto.TopicPredictionResponse(strong, average, weak, topics);
    }

    @Override
    public List<PredictionDto.PredictionHistoryDto> predictionHistory(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        List<Prediction> predictions = predictionRepository.findByStudentOrderByCreatedAtDesc(student);
        List<Test> tests = testRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .filter(t -> t.getStatus() == Test.Status.COMPLETED
                        && t.getPercentage() != null && t.getSubmittedAt() != null)
                .sorted(Comparator.comparing(Test::getSubmittedAt))
                .toList();

        List<PredictionDto.PredictionHistoryDto> rows = new ArrayList<>();
        for (Prediction p : predictions) {
            String subject = "Overall";
            BigDecimal actual = null;
            for (Test t : tests) {
                if (t.getSubmittedAt().isAfter(p.getCreatedAt())) {
                    actual = t.getPercentage();
                    subject = subjectFor(t);
                    break;
                }
            }
            BigDecimal difference = actual == null ? null : actual.subtract(p.getPredictedScore());
            rows.add(new PredictionDto.PredictionHistoryDto(
                    p.getCreatedAt(), subject, p.getPredictedScore(), actual, difference));
            if (rows.size() >= 10) break;
        }
        return rows;
    }

    private String subjectFor(Test test) {
        List<TestQuestion> tqs = testQuestionRepository.findByTestOrderByOrderNoAsc(test);
        if (tqs.isEmpty()) return "Overall";
        Topic topic = tqs.get(0).getQuestion().getTopic();
        if (topic != null && topic.getSubject() != null) {
            return topic.getSubject().getSubjectName();
        }
        return "Overall";
    }

    @Override
    public Map<String, Object> explain(Long studentId, String method) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        Map<String, Object> features = buildFeatures(student);
        Map<String, Object> result = "lime".equalsIgnoreCase(method)
                ? aiServiceClient.explainLime(features)
                : aiServiceClient.explainShap(features);
        Map<String, Object> out = new HashMap<>(result);
        out.put("method", method);
        return out;
    }

    @Override
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("available", true);
        result.put("models", List.of("random_forest", "gradient_boosting", "xgboost", "decision_tree"));
        result.put("service", "spring-service");
        return result;
    }

    private Map<String, Object> buildFeatures(Student student) {
        List<QuestionAttempt> attempts = attemptRepository.findByStudent(student);
        long total = attempts.size();
        long correct = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        long incorrect = total - correct;
        Map<String, Long> subjectCorrect = new HashMap<>();
        Map<String, Long> subjectTotal = new HashMap<>();
        for (QuestionAttempt a : attempts) {
            String code = a.getQuestion().getTopic().getSubject().getSubjectCode();
            subjectTotal.merge(code, 1L, Long::sum);
            if (Boolean.TRUE.equals(a.getCorrect())) {
                subjectCorrect.merge(code, 1L, Long::sum);
            }
        }
        double avgAttendance = 85.0 + (student.getXpPoints() % 15);
        double studyHours = Math.min(10, 4 + student.getStreakDays() * 0.5);

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("cgpa", student.getCgpa() == null ? 7.0 : student.getCgpa().doubleValue());
        features.put("attendance", avgAttendance);
        features.put("study_hours_per_week", studyHours);
        features.put("assignments_completed", Math.min(100, 60 + correct));
        features.put("previous_test_score", 65 + (student.getLevel() * 3) % 30);
        features.put("total_questions_attempted", total);
        features.put("total_correct", correct);
        features.put("total_incorrect", incorrect);
        features.put("accuracy", total == 0 ? 0 : correct * 100.0 / total);
        features.put("semester", student.getSemester());
        features.put("streak_days", student.getStreakDays());
        features.put("participation_score", Math.min(100, 40 + student.getXpPoints() / 10));
        for (Map.Entry<String, Long> e : subjectCorrect.entrySet()) {
            long t = subjectTotal.getOrDefault(e.getKey(), 0L);
            features.put("subject_" + e.getKey().toLowerCase(), t == 0 ? 0 : e.getValue() * 100.0 / t);
        }
        return features;
    }

    private Map<String, Double> buildLocalImportance(Student student) {
        Map<String, Double> imp = new LinkedHashMap<>();
        imp.put("cgpa", 0.35);
        imp.put("accuracy", 0.20);
        imp.put("study_hours_per_week", 0.15);
        imp.put("attendance", 0.12);
        imp.put("previous_test_score", 0.10);
        imp.put("assignments_completed", 0.08);
        return imp;
    }

    private String buildLocalExplanation(BigDecimal score, String risk) {
        return "Based on current academic data, the predicted score is " + score
                + " with a " + risk.toLowerCase() + " risk level. "
                + (risk.equalsIgnoreCase("LOW")
                    ? "Performance is on track. Maintain study habits."
                    : "Focus on weak subjects and improve attendance and practice frequency.");
    }

    private BigDecimal fallbackScore(Student student) {
        double base = student.getCgpa() == null ? 65 : student.getCgpa().doubleValue() * 8;
        double bonus = Math.min(20, student.getStreakDays() * 1.5);
        double score = Math.min(100, base + bonus);
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private String gradeFor(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 90) return "A+";
        if (s >= 80) return "A";
        if (s >= 70) return "B+";
        if (s >= 60) return "B";
        if (s >= 50) return "C";
        if (s >= 40) return "D";
        return "F";
    }

    private String riskFor(BigDecimal score) {
        double s = score.doubleValue();
        if (s >= 75) return "LOW";
        if (s >= 60) return "MEDIUM";
        if (s >= 45) return "HIGH";
        return "CRITICAL";
    }

    private KnowledgeGap.GapLevel gapLevelFor(double accuracy) {
        if (accuracy >= 80) return KnowledgeGap.GapLevel.LOW;
        if (accuracy >= 60) return KnowledgeGap.GapLevel.MEDIUM;
        if (accuracy >= 40) return KnowledgeGap.GapLevel.HIGH;
        return KnowledgeGap.GapLevel.CRITICAL;
    }

    private double accuracyFor(List<QuestionAttempt> attempts, Question.Difficulty difficulty) {
        List<QuestionAttempt> filtered = attempts.stream()
                .filter(a -> a.getQuestion().getDifficulty() == difficulty)
                .toList();
        if (filtered.isEmpty()) return 0;
        long c = filtered.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
        return c * 100.0 / filtered.size();
    }

    private String pct(double v) {
        return Math.round(v) + "%";
    }

    private String signPct(double v) {
        return (v >= 0 ? "+" : "") + Math.round(v) + "%";
    }

    private String statusFor(double v) {
        if (v >= 75) return "GOOD";
        if (v >= 50) return "AVERAGE";
        return "POOR";
    }

    private String statusForImprovement(double v) {
        if (v >= 5) return "GOOD";
        if (v >= -5) return "AVERAGE";
        return "POOR";
    }

    private String consistencyStatus(String consistency) {
        if ("Good".equals(consistency)) return "GOOD";
        if ("Average".equals(consistency)) return "AVERAGE";
        return "POOR";
    }

    private PredictionDto.PredictionResponse toResponse(Prediction p,
                                                        List<PredictionDto.WeakSubjectDto> weak,
                                                        List<PredictionDto.KnowledgeGapDto> gaps) {
        Map<String, Double> importance = new LinkedHashMap<>();
        if (p.getFeatureImportance() != null) {
            try {
                Map<?, ?> map = objectMapper.readValue(p.getFeatureImportance(), Map.class);
                map.forEach((k, v) -> importance.put(String.valueOf(k), ((Number) v).doubleValue()));
            } catch (Exception ignored) {
            }
        }
        return new PredictionDto.PredictionResponse(
                p.getPredictionId(), p.getModelName(), p.getPredictedGrade(),
                p.getPredictedScore(), p.getRiskLevel().name(), p.getConfidenceScore(),
                importance, p.getExplanation(), weak, gaps, p.getCreatedAt());
    }
}
