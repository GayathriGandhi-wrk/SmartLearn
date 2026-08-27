package com.student.performance.service;

import com.student.performance.dto.AdminDto;

import java.util.List;

public interface AdminService {

    AdminDto.DashboardStats getDashboardStats();

    List<AdminDto.UserResponse> listUsers(String role);

    void toggleUserStatus(Long userId);

    AdminDto.StudentOverviewResponse getStudentOverview(Long studentId);

    long importQuestions(List<AdminDto.QuestionImportItem> items);
}
