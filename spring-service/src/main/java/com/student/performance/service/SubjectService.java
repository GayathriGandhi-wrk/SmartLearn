package com.student.performance.service;

import com.student.performance.dto.SubjectDto;

import java.util.List;

public interface SubjectService {

    List<SubjectDto.SubjectResponse> listSubjects(String department);

    SubjectDto.SubjectResponse createSubject(SubjectDto.SubjectRequest request);

    SubjectDto.TopicResponse createTopic(Long subjectId, SubjectDto.TopicRequest request);

    List<SubjectDto.TopicResponse> listTopics(Long subjectId);

    List<SubjectDto.SubjectResponse> listBySemester(Integer semester);

    SubjectDto.PerformanceResponse getPerformance(Long studentId);

    void saveSubjectMarks(List<SubjectDto.SubjectMarksRequest> requests, Long studentId);

    void updateStudentStreak(Long studentId);
}
