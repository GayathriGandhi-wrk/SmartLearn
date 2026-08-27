package com.student.performance.service;

import com.student.performance.dto.PredictionDto;

import java.util.List;
import java.util.Map;

public interface PredictionService {

    PredictionDto.PredictionResponse predict(Long studentId);

    PredictionDto.PredictionResponse getLatestPrediction(Long studentId);

    List<PredictionDto.WeakSubjectDto> detectWeakSubjects(Long studentId);

    List<PredictionDto.KnowledgeGapDto> detectKnowledgeGaps(Long studentId);

    PredictionDto.PerformanceFactorsResponse performanceFactors(Long studentId);

    PredictionDto.TopicPredictionResponse topicPrediction(Long studentId);

    List<PredictionDto.PredictionHistoryDto> predictionHistory(Long studentId);

    Map<String, Object> explain(Long studentId, String method);

    Map<String, Object> healthCheck();
}
