package com.student.performance.service.impl;

import com.student.performance.dto.PredictionDto;
import com.student.performance.entity.ChatHistory;
import com.student.performance.entity.Student;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.ChatHistoryRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.service.AiServiceClient;
import com.student.performance.service.ChatbotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final StudentRepository studentRepository;
    private final AiServiceClient aiServiceClient;

    public ChatbotServiceImpl(ChatHistoryRepository chatHistoryRepository,
                              StudentRepository studentRepository,
                              AiServiceClient aiServiceClient) {
        this.chatHistoryRepository = chatHistoryRepository;
        this.studentRepository = studentRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    @Transactional
    public PredictionDto.ChatbotResponse chat(Long studentId, String message, String provider,
                                              List<PredictionDto.ChatMessage> history) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (message == null || message.isBlank()) {
            message = "Explain the concept of binary search";
        }

        List<Map<String, String>> chatHistory = null;
        if (history != null && !history.isEmpty()) {
            chatHistory = history.stream()
                    .filter(h -> h.content() != null && h.role() != null)
                    .map(h -> Map.of("role", h.role(), "content", h.content()))
                    .toList();
        }

        Map<String, Object> response = aiServiceClient.chat(message, provider, chatHistory);
        String reply;
        String intent = detectIntent(message);
        BigDecimal confidence = BigDecimal.valueOf(0.95);

        if (Boolean.FALSE.equals(response.get("available"))) {
            reply = localFallbackReply(message);
            confidence = BigDecimal.valueOf(0.85);
        } else if (response.get("reply") != null) {
            reply = response.get("reply").toString();
            if (response.get("confidence") != null) {
                try {
                    confidence = new BigDecimal(response.get("confidence").toString());
                } catch (NumberFormatException ignored) {
                }
            }
            if (response.get("intent") != null) {
                intent = response.get("intent").toString();
            }
        } else {
            reply = localFallbackReply(message);
            confidence = BigDecimal.valueOf(0.85);
        }

        ChatHistory chat = new ChatHistory();
        chat.setStudent(student);
        chat.setUserMessage(message);
        chat.setBotResponse(reply);
        chat.setIntent(intent);
        chat.setConfidence(confidence);
        chatHistoryRepository.save(chat);

        return new PredictionDto.ChatbotResponse(reply, intent, confidence, LocalDateTime.now());
    }

    public List<ChatHistory> getHistory(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        return chatHistoryRepository.findTop50ByStudentOrderByCreatedAtDesc(student);
    }

    private String detectIntent(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("concept") || m.contains("explain") || m.contains("what is") || m.contains("how does")) {
            return "CONCEPT_EXPLANATION";
        }
        if (m.contains("study") || m.contains("plan") || m.contains("schedule")) {
            return "STUDY_GUIDANCE";
        }
        if (m.contains("doubt") || m.contains("confused") || m.contains("not clear")) {
            return "DOUBT_SOLVING";
        }
        if (m.contains("predict") || m.contains("grade") || m.contains("score") || m.contains("risk")) {
            return "PERFORMANCE_INSIGHT";
        }
        return "GENERAL";
    }

    private String localFallbackReply(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        if (m.contains("binary search")) {
            return "Binary search is an O(log n) algorithm that repeatedly divides a sorted array in half, comparing the target with the middle element to decide which half to search next.";
        }
        if (m.contains("study") || m.contains("plan")) {
            return "I recommend: 1) Revise your weakest topics first, 2) Attempt 15-20 practice questions daily, 3) Take one adaptive test every day, 4) Review your wrong answers before bed.";
        }
        if (m.contains("predict") || m.contains("grade")) {
            return "Go to the AI Prediction page to see your predicted grade and risk level. Focus on improving attendance and daily practice to boost your score.";
        }
        if (m.contains("hello") || m.contains("hi")) {
            return "Hello! I'm your AI study assistant. Ask me about concepts, study plans, doubts, or your performance predictions.";
        }
        return "That's a great question! For a detailed explanation, I'd suggest reviewing the related topic in the Subjects section and then practicing with the question bank. You can also ask me about specific concepts like 'binary search' or 'normalization'.";
    }
}
