package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.PredictionDto;
import com.student.performance.service.PredictionService;
import com.student.performance.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@Tag(name = "Prediction & AI", description = "Performance prediction, weak subjects, knowledge gaps and XAI")
public class PredictionController {

    private final PredictionService predictionService;
    private final SecurityUtils securityUtils;

    public PredictionController(PredictionService predictionService, SecurityUtils securityUtils) {
        this.predictionService = predictionService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/predict")
    @Operation(summary = "Predict current student performance (grade, risk, confidence)")
    public ResponseEntity<ApiResponse<PredictionDto.PredictionResponse>> predict() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.predict(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/prediction/latest")
    @Operation(summary = "Get the latest stored prediction")
    public ResponseEntity<ApiResponse<PredictionDto.PredictionResponse>> latest() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.getLatestPrediction(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/weak-subjects")
    @Operation(summary = "Detect weak subjects for the current student")
    public ResponseEntity<ApiResponse<List<PredictionDto.WeakSubjectDto>>> weakSubjects() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.detectWeakSubjects(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/knowledge-gaps")
    @Operation(summary = "Detect knowledge gaps by topic")
    public ResponseEntity<ApiResponse<List<PredictionDto.KnowledgeGapDto>>> gaps() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.detectKnowledgeGaps(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/performance-factors")
    @Operation(summary = "Get performance factors that explain the predicted score")
    public ResponseEntity<ApiResponse<PredictionDto.PerformanceFactorsResponse>> factors() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.performanceFactors(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/topic-prediction")
    @Operation(summary = "Predict scores by topic with strong/average/weak categories")
    public ResponseEntity<ApiResponse<PredictionDto.TopicPredictionResponse>> topicPrediction() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.topicPrediction(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/prediction-history")
    @Operation(summary = "Prediction history with actual test scores")
    public ResponseEntity<ApiResponse<List<PredictionDto.PredictionHistoryDto>>> predictionHistory() {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.predictionHistory(securityUtils.getCurrentStudentId())));
    }

    @GetMapping("/explain")
    @Operation(summary = "SHAP / LIME explanation of prediction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> explain(
            @RequestParam(defaultValue = "shap") String method) {
        return ResponseEntity.ok(ApiResponse.ok(
                predictionService.explain(securityUtils.getCurrentStudentId(), method)));
    }

    @GetMapping("/ping")
    @Operation(summary = "Health check for the AI service proxy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        return ResponseEntity.ok(ApiResponse.ok(predictionService.healthCheck()));
    }
}
