package com.student.performance.service.impl;

import com.student.performance.dto.QuestionDto;
import com.student.performance.entity.Question;
import com.student.performance.entity.PracticeQuestion;
import com.student.performance.entity.QuestionAttempt;
import com.student.performance.entity.Student;
import com.student.performance.entity.Topic;
import com.student.performance.exception.BadRequestException;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.PracticeQuestionRepository;
import com.student.performance.repository.QuestionAttemptRepository;
import com.student.performance.repository.QuestionRepository;
import com.student.performance.repository.TopicRepository;
import com.student.performance.service.QuestionService;
import com.student.performance.util.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final PracticeQuestionRepository practiceRepository;
    private final QuestionAttemptRepository attemptRepository;
    private final SecurityUtils securityUtils;

    public QuestionServiceImpl(QuestionRepository questionRepository,
                               TopicRepository topicRepository,
                               PracticeQuestionRepository practiceRepository,
                               QuestionAttemptRepository attemptRepository,
                               SecurityUtils securityUtils) {
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.practiceRepository = practiceRepository;
        this.attemptRepository = attemptRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public QuestionDto.QuestionBankResponse getQuestionBank(Long subjectId, Long topicId,
                                                            Question.Difficulty difficulty,
                                                            int page, int size, String search) {
        List<Question> all = questionRepository.findAll();
        List<QuestionDto.QuestionResponse> filtered = all.stream()
                .filter(Question::isActive)
                .filter(q -> subjectId == null || q.getTopic().getSubject().getSubjectId().equals(subjectId))
                .filter(q -> topicId == null || q.getTopic().getTopicId().equals(topicId))
                .filter(q -> difficulty == null || q.getDifficulty() == difficulty)
                .filter(q -> search == null || search.isBlank()
                        || q.getQuestionText().toLowerCase().contains(search.toLowerCase()))
                .map(QuestionServiceImpl::toResponse)
                .collect(Collectors.toList());

        long total = filtered.size();
        long begin = filtered.stream().filter(q -> q.difficulty() == Question.Difficulty.BEGINNER).count();
        long inter = filtered.stream().filter(q -> q.difficulty() == Question.Difficulty.INTERMEDIATE).count();
        long adv = filtered.stream().filter(q -> q.difficulty() == Question.Difficulty.ADVANCED).count();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        List<QuestionDto.QuestionResponse> pageContent = filtered.isEmpty() ? List.of() : filtered.subList(from, to);

        return new QuestionDto.QuestionBankResponse(total, begin, inter, adv, pageContent);
    }

    @Override
    public QuestionDto.QuestionResponse getQuestion(Long questionId) {
        return toResponse(questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId)));
    }

    @Override
    @Transactional
    public QuestionDto.QuestionResponse createQuestion(QuestionDto.QuestionRequest request) {
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.topicId()));
        Question q = new Question();
        apply(q, request, topic);
        return toResponse(questionRepository.save(q));
    }

    @Override
    @Transactional
    public QuestionDto.QuestionResponse updateQuestion(Long questionId, QuestionDto.QuestionRequest request) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.topicId()));
        apply(q, request, topic);
        return toResponse(questionRepository.save(q));
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        q.setActive(false);
        questionRepository.save(q);
    }

    @Override
    public long countBeginner() {
        return questionRepository.countByDifficulty(Question.Difficulty.BEGINNER);
    }

    @Override
    public long countIntermediate() {
        return questionRepository.countByDifficulty(Question.Difficulty.INTERMEDIATE);
    }

    @Override
    public long countAdvanced() {
        return questionRepository.countByDifficulty(Question.Difficulty.ADVANCED);
    }

    @Override
    public List<QuestionDto.QuestionResponse> generateDailyPractice(int count) {
        List<Question> pool = new ArrayList<>();
        pool.addAll(questionRepository.findRandomByDifficulty(Question.Difficulty.BEGINNER));
        pool.addAll(questionRepository.findRandomByDifficulty(Question.Difficulty.INTERMEDIATE));
        pool.addAll(questionRepository.findRandomByDifficulty(Question.Difficulty.ADVANCED));
        return pool.stream().limit(count).map(QuestionServiceImpl::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestionDto.PracticeResponse toggleBookmark(Long questionId) {
        Student student = securityUtils.getCurrentStudent();
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        PracticeQuestion pq = practiceRepository.findByStudentAndQuestion(student, q)
                .orElseGet(() -> {
                    PracticeQuestion p = new PracticeQuestion();
                    p.setStudent(student);
                    p.setQuestion(q);
                    return p;
                });
        pq.setBookmarked(!pq.isBookmarked());
        pq.setLastPracticedAt(java.time.LocalDateTime.now());
        return toPracticeResponse(practiceRepository.save(pq));
    }

    @Override
    public List<QuestionDto.PracticeResponse> getBookmarked() {
        Student student = securityUtils.getCurrentStudent();
        return practiceRepository.findByStudentAndBookmarkedTrue(student).stream()
                .map(QuestionServiceImpl::toPracticeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordAttempt(Long questionId, Question.Answer selectedAnswer, Integer timeTakenSec) {
        Student student = securityUtils.getCurrentStudent();
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        if (selectedAnswer == null) {
            throw new BadRequestException("Selected answer is required");
        }
        QuestionAttempt attempt = new QuestionAttempt();
        attempt.setStudent(student);
        attempt.setQuestion(q);
        attempt.setSelectedAnswer(selectedAnswer);
        attempt.setCorrect(selectedAnswer == q.getCorrectAnswer());
        attempt.setTimeTakenSec(timeTakenSec);
        attemptRepository.save(attempt);

        PracticeQuestion pq = practiceRepository.findByStudentAndQuestion(student, q).orElseGet(() -> {
            PracticeQuestion p = new PracticeQuestion();
            p.setStudent(student);
            p.setQuestion(q);
            return p;
        });
        pq.setSolved(true);
        pq.setAttemptCount(pq.getAttemptCount() + 1);
        pq.setLastPracticedAt(java.time.LocalDateTime.now());
        practiceRepository.save(pq);
    }

    private void apply(Question q, QuestionDto.QuestionRequest r, Topic topic) {
        q.setTopic(topic);
        q.setQuestionText(r.questionText());
        q.setOptionA(r.optionA());
        q.setOptionB(r.optionB());
        q.setOptionC(r.optionC());
        q.setOptionD(r.optionD());
        q.setCorrectAnswer(r.correctAnswer());
        q.setExplanation(r.explanation());
        q.setDifficulty(r.difficulty() == null ? Question.Difficulty.BEGINNER : r.difficulty());
        q.setMarks(r.marks() == null ? java.math.BigDecimal.ONE : r.marks());
        q.setQuestionType(r.questionType() == null ? "MCQ" : r.questionType());
        q.setActive(true);
    }

    public static QuestionDto.QuestionResponse toResponse(Question q) {
        return new QuestionDto.QuestionResponse(
                q.getQuestionId(), q.getTopic().getTopicId(), q.getTopic().getTopicName(),
                q.getTopic().getSubject().getSubjectId(), q.getTopic().getSubject().getSubjectName(),
                q.getQuestionText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getDifficulty(), q.getExplanation(), q.getMarks(), q.getQuestionType());
    }

    public static QuestionDto.PracticeResponse toPracticeResponse(PracticeQuestion p) {
        Question q = p.getQuestion();
        return new QuestionDto.PracticeResponse(
                p.getPracticeId(), q.getQuestionId(), q.getQuestionText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getDifficulty(), q.getExplanation(), p.isBookmarked(), p.isSolved(), p.getAttemptCount());
    }
}
