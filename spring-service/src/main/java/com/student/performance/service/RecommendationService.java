package com.student.performance.service;

import com.student.performance.dto.PredictionDto;

import java.util.List;

public interface RecommendationService {

    List<PredictionDto.RecommendationDto> generateRecommendations(Long studentId);

    List<PredictionDto.RecommendationDto> getRecommendations(Long studentId, boolean unreadOnly);

    void markViewed(Long recommendationId);

    long unreadCount(Long studentId);
}
