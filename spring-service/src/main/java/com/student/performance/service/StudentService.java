package com.student.performance.service;

import com.student.performance.dto.StudentDto;

public interface StudentService {

    StudentDto.StudentProfileResponse getProfile();

    StudentDto.StudentProfileResponse updateProfile(StudentDto.ProfileUpdateRequest request);

    StudentDto.StudentProfileResponse updateAcademic(StudentDto.AcademicDetailsRequest request);

    StudentDto.StudentProfileResponse uploadProfilePhoto(org.springframework.web.multipart.MultipartFile file);

    void deleteAccount();

    void changePassword(String oldPassword, String newPassword);
}
