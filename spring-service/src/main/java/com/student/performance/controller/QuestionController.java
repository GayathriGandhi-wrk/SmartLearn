package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.QuestionDto;
import com.student.performance.entity.Question;
import com.student.performance.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@Tag(name = "Question Bank", description = "Question bank browsing, practice and bookmarks")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    @Operation(summary = "Browse question bank with filters")
    public ResponseEntity<ApiResponse<QuestionDto.QuestionBankResponse>> browse(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Question.Difficulty difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                questionService.getQuestionBank(subjectId, topicId, difficulty, page, size, search)));
    }

    @GetMapping("/counts")
    @Operation(summary = "Question counts by difficulty")
    public ResponseEntity<ApiResponse<QuestionDto.QuestionBankResponse>> counts() {
        return ResponseEntity.ok(ApiResponse.ok(new QuestionDto.QuestionBankResponse(
                questionService.countBeginner() + questionService.countIntermediate() + questionService.countAdvanced(),
                questionService.countBeginner(), questionService.countIntermediate(), questionService.countAdvanced(), List.of())));
    }

    @GetMapping("/{questionId}")
    @Operation(summary = "Get a single question")
    public ResponseEntity<ApiResponse<QuestionDto.QuestionResponse>> get(@PathVariable Long questionId) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.getQuestion(questionId)));
    }

    @PostMapping
    @Operation(summary = "Create a new question (admin)")
    public ResponseEntity<ApiResponse<QuestionDto.QuestionResponse>> create(@RequestBody QuestionDto.QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.createQuestion(request)));
    }

    @PutMapping("/{questionId}")
    @Operation(summary = "Update a question (admin)")
    public ResponseEntity<ApiResponse<QuestionDto.QuestionResponse>> update(@PathVariable Long questionId,
                                                                            @RequestBody QuestionDto.QuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.updateQuestion(questionId, request)));
    }

    @DeleteMapping("/{questionId}")
    @Operation(summary = "Soft-delete a question (admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long questionId) {
        questionService.deleteQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.ok("Question deleted", null));
    }

    @GetMapping("/practice/daily")
    @Operation(summary = "Generate daily practice questions")
    public ResponseEntity<ApiResponse<List<QuestionDto.QuestionResponse>>> daily(
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.generateDailyPractice(count)));
    }

    @PostMapping("/{questionId}/bookmark")
    @Operation(summary = "Toggle bookmark for a question")
    public ResponseEntity<ApiResponse<QuestionDto.PracticeResponse>> bookmark(@PathVariable Long questionId) {
        return ResponseEntity.ok(ApiResponse.ok(questionService.toggleBookmark(questionId)));
    }

    @GetMapping("/bookmarks")
    @Operation(summary = "Get bookmarked questions")
    public ResponseEntity<ApiResponse<List<QuestionDto.PracticeResponse>>> bookmarks() {
        return ResponseEntity.ok(ApiResponse.ok(questionService.getBookmarked()));
    }

    @PostMapping("/{questionId}/attempt")
    @Operation(summary = "Record a practice attempt")
    public ResponseEntity<ApiResponse<Void>> attempt(@PathVariable Long questionId,
                                                     @RequestBody QuestionDto.AttemptRequest request) {
        questionService.recordAttempt(questionId, request.selectedAnswer(), request.timeTakenSec());
        return ResponseEntity.ok(ApiResponse.ok("Attempt recorded", null));
    }
}
