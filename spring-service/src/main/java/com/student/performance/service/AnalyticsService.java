package com.student.performance.service;

import com.student.performance.dto.AnalyticsDto;

public interface AnalyticsService {

    AnalyticsDto.AnalyticsResponse getAnalytics(Long studentId);
}
