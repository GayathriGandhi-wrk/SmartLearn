package com.student.performance.service;

import com.student.performance.dto.PlannerDto;

import java.util.List;

public interface PlannerService {

    PlannerDto.StudyPlanSummaryDto generatePlan(Long studentId, PlannerDto.StudyPlanRequest request);

    List<PlannerDto.StudyPlanSummaryDto> getPlans(Long studentId);

    PlannerDto.StudyPlanSummaryDto getPlan(Long studentId, Long planId);

    PlannerDto.StudyPlanSummaryDto regeneratePlan(Long studentId, Long planId, PlannerDto.StudyPlanRequest request);

    void deletePlan(Long studentId, Long planId);

    PlannerDto.SessionDto completeSession(Long studentId, Long sessionId);

    PlannerDto.SessionDto skipSession(Long studentId, Long sessionId);

    PlannerDto.StudyPlanSummaryDto getReport(Long studentId, Long planId);
}
