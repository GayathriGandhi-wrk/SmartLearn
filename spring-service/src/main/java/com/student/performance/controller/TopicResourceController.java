package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.TopicResourceDto;
import com.student.performance.service.TopicResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/topic-resources")
@Tag(name = "Topic Resources", description = "Track tutorials a student opens and quiz them on what they learned")
public class TopicResourceController {

    private final TopicResourceService topicResourceService;

    public TopicResourceController(TopicResourceService topicResourceService) {
        this.topicResourceService = topicResourceService;
    }

    @PostMapping("/{topicId}/views")
    @Operation(summary = "Record that the student opened a tutorial, doc or video")
    public ResponseEntity<ApiResponse<TopicResourceDto.ViewResponse>> recordView(
            @PathVariable Long topicId,
            @RequestBody TopicResourceDto.ViewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(topicResourceService.recordView(topicId, request)));
    }

    @GetMapping("/{topicId}/views")
    @Operation(summary = "List the resources this student has opened for a topic")
    public ResponseEntity<ApiResponse<List<TopicResourceDto.ViewResponse>>> listViews(
            @PathVariable Long topicId) {
        return ResponseEntity.ok(ApiResponse.ok(topicResourceService.listViews(topicId)));
    }

    @PostMapping("/{topicId}/questions")
    @Operation(summary = "'I have learned up to here' - generate questions the student has not seen before")
    public ResponseEntity<ApiResponse<TopicResourceDto.GenerateResponse>> generate(
            @PathVariable Long topicId,
            @RequestBody(required = false) TopicResourceDto.GenerateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(topicResourceService.generate(topicId, request)));
    }

    @PostMapping("/questions/{questionId}/answer")
    @Operation(summary = "Submit an answer to a generated question")
    public ResponseEntity<ApiResponse<TopicResourceDto.AnswerResponse>> answer(
            @PathVariable Long questionId,
            @RequestBody TopicResourceDto.AnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(topicResourceService.answer(questionId, request)));
    }
}
