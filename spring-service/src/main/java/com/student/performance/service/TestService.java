package com.student.performance.service;

import com.student.performance.dto.TestDto;

public interface TestService {

    TestDto.ActiveTestResponse startTest(Long studentId, TestDto.StartTestRequest request);

    TestDto.ActiveTestResponse getActiveTest(Long studentId);

    TestDto.TestResultDto submitTest(Long testId, Long studentId, TestDto.SubmitTestRequest request);

    TestDto.TestResultDto getResult(Long testId, Long studentId);

    java.util.List<TestDto.TestListItem> getTestHistory(Long studentId);

    java.util.List<TestDto.ReviewedTestQuestion> getReviewedTestQuestions(Long studentId);

    void expireTimedOutTests();
}
