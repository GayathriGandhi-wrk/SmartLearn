package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.PredictionDto;
import com.student.performance.service.ChatbotService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@Tag(name = "AI Chatbot", description = "Doubt solving, concept explanation and study guidance")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final SecurityUtils securityUtils;

    public ChatbotController(ChatbotService chatbotService, SecurityUtils securityUtils) {
        this.chatbotService = chatbotService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/chat")
    @Operation(summary = "Send a message to the AI chatbot")
    public ResponseEntity<ApiResponse<PredictionDto.ChatbotResponse>> chat(
            @RequestBody PredictionDto.ChatbotRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                chatbotService.chat(securityUtils.getCurrentStudentId(),
                        request.message(), request.provider(), request.history())));
    }
}
