package com.student.performance.service.impl;

import com.student.performance.dto.PredictionDto;
import com.student.performance.entity.KnowledgeGap;
import com.student.performance.entity.Recommendation;
import com.student.performance.entity.Student;
import com.student.performance.entity.WeakSubject;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.KnowledgeGapRepository;
import com.student.performance.repository.RecommendationRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.repository.WeakSubjectRepository;
import com.student.performance.service.RecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final WeakSubjectRepository weakSubjectRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;
    private final StudentRepository studentRepository;

    public RecommendationServiceImpl(RecommendationRepository recommendationRepository,
                                     WeakSubjectRepository weakSubjectRepository,
                                     KnowledgeGapRepository knowledgeGapRepository,
                                     StudentRepository studentRepository) {
        this.recommendationRepository = recommendationRepository;
        this.weakSubjectRepository = weakSubjectRepository;
        this.knowledgeGapRepository = knowledgeGapRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public List<PredictionDto.RecommendationDto> generateRecommendations(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        List<Recommendation> generated = new ArrayList<>();
        List<WeakSubject> weakSubjects = weakSubjectRepository.findByStudentOrderByWeaknessScoreAsc(student);
        List<KnowledgeGap> gaps = knowledgeGapRepository.findByStudentOrderByMasteryLevelAsc(student);

        for (int i = 0; i < Math.min(3, weakSubjects.size()); i++) {
            WeakSubject ws = weakSubjects.get(i);
            String sub = ws.getSubject().getSubjectName();
            int priority = i + 1;
            BigDecimal avg = ws.getAverageMarks() == null ? BigDecimal.ZERO : ws.getAverageMarks();
            generated.add(recommendation(
                    student, "STUDY_MATERIAL", "Strengthen " + sub,
                    "Focus on " + sub + ". Your current accuracy is "
                            + avg + "%. Practice daily to improve.",
                    Recommendation.ResourceType.MATERIAL,
                    "https://www.geeksforgeeks.org/search?q=" + encode(sub),
                    priority, "Weak subject detected with high weakness score"));
            generated.add(recommendation(
                    student, "PRACTICE", "Practice Questions - " + sub,
                    "Solve at least 20 practice questions on " + sub + " to raise accuracy above 70%.",
                    Recommendation.ResourceType.QUESTION,
                    "/questions?subjectId=" + ws.getSubject().getSubjectId(),
                    priority, "Low accuracy on this subject"));
        }

        for (int i = 0; i < Math.min(3, gaps.size()); i++) {
            KnowledgeGap gap = gaps.get(i);
            String topic = gap.getTopic().getTopicName();
            generated.add(recommendation(
                    student, "REVISION", "Revise - " + topic,
                    "Knowledge gap detected in " + topic + " (mastery "
                            + gap.getMasteryLevel() + "%). Allocate "
                            + gap.getRecommendedHours() + " hours this week.",
                    Recommendation.ResourceType.REVISION,
                    "/topics?topicId=" + gap.getTopic().getTopicId(),
                    i + 1, "Knowledge gap level " + gap.getGapLevel().name()));
        }

        generated.add(recommendation(
                student, "VIDEO", "Learning Strategies Video",
                "Watch curated video lectures on your weakest topics to build conceptual clarity.",
                Recommendation.ResourceType.VIDEO,
                "https://www.youtube.com/results?search_query=" + encode("study tips computer science"),
                4, "Recommended to boost conceptual understanding"));
        recommendationRepository.saveAll(generated);
        return generated.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<PredictionDto.RecommendationDto> getRecommendations(Long studentId, boolean unreadOnly) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        List<Recommendation> list = unreadOnly
                ? recommendationRepository.findByStudentAndViewedFalseOrderByPriorityAsc(student)
                : recommendationRepository.findByStudentOrderByPriorityAscCreatedAtDesc(student);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markViewed(Long recommendationId) {
        Recommendation r = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + recommendationId));
        r.setViewed(true);
        recommendationRepository.save(r);
    }

    @Override
    public long unreadCount(Long studentId) {
        return recommendationRepository.countByStudent_StudentIdAndViewedFalse(studentId);
    }

    private Recommendation recommendation(Student student, String type, String title,
                                           String description, Recommendation.ResourceType resourceType,
                                           String url, int priority, String reason) {
        Recommendation r = new Recommendation();
        r.setStudent(student);
        r.setType(type);
        r.setTitle(title);
        r.setDescription(description);
        r.setResourceType(resourceType);
        r.setResourceUrl(url);
        r.setPriority(priority);
        r.setReason(reason);
        r.setViewed(false);
        return r;
    }

    private PredictionDto.RecommendationDto toDto(Recommendation r) {
        return new PredictionDto.RecommendationDto(
                r.getRecommendationId(), r.getType(), r.getTitle(), r.getDescription(),
                r.getResourceType() == null ? null : r.getResourceType().name(),
                r.getResourceUrl(), r.getPriority(), r.getReason(), r.isViewed());
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
