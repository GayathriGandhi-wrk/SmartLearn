package com.student.performance.service;

import com.student.performance.dto.PredictionDto;

import java.util.List;

public interface ChatbotService {

    PredictionDto.ChatbotResponse chat(Long studentId, String message, String provider,
                                       List<PredictionDto.ChatMessage> history);
}
