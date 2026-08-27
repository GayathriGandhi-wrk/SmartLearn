package com.student.performance.service;

import com.student.performance.dto.QuestionDto;
import com.student.performance.entity.Question;

import java.util.List;

public interface QuestionService {

    QuestionDto.QuestionBankResponse getQuestionBank(Long subjectId, Long topicId,
                                                     Question.Difficulty difficulty, int page, int size, String search);

    QuestionDto.QuestionResponse getQuestion(Long questionId);

    QuestionDto.QuestionResponse createQuestion(QuestionDto.QuestionRequest request);

    QuestionDto.QuestionResponse updateQuestion(Long questionId, QuestionDto.QuestionRequest request);

    void deleteQuestion(Long questionId);

    long countBeginner();

    long countIntermediate();

    long countAdvanced();

    List<QuestionDto.QuestionResponse> generateDailyPractice(int count);

    QuestionDto.PracticeResponse toggleBookmark(Long questionId);

    List<QuestionDto.PracticeResponse> getBookmarked();

    void recordAttempt(Long questionId, Question.Answer selectedAnswer, Integer timeTakenSec);
}
