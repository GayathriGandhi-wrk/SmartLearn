package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prediction_id")
    private Long predictionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "predicted_grade", nullable = false, length = 5)
    private String predictedGrade;

    @Column(name = "predicted_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal predictedScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "feature_importance", columnDefinition = "JSON")
    private String featureImportance;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "input_features", columnDefinition = "JSON")
    private String inputFeatures;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
