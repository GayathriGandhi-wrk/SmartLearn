package com.student.performance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "test_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_question_id")
    private Long testQuestionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_answer", length = 1)
    private Question.Answer selectedAnswer;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "marks_obtained", precision = 4, scale = 2)
    private BigDecimal marksObtained;

    @Column(name = "time_taken_sec")
    private Integer timeTakenSec;

    @Column(name = "is_answered", nullable = false)
    private boolean answered = false;
}
