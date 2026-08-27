package com.student.performance.controller;

import com.student.performance.dto.ApiResponse;
import com.student.performance.dto.StudentDto;
import com.student.performance.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/students")
@Tag(name = "Student", description = "Student profile and academic management")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current student profile")
    public ResponseEntity<ApiResponse<StudentDto.StudentProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getProfile()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update student profile")
    public ResponseEntity<ApiResponse<StudentDto.StudentProfileResponse>> updateProfile(
            @Valid @RequestBody StudentDto.ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.updateProfile(request)));
    }

    @PutMapping("/me/academic")
    @Operation(summary = "Update academic details")
    public ResponseEntity<ApiResponse<StudentDto.StudentProfileResponse>> updateAcademic(
            @Valid @RequestBody StudentDto.AcademicDetailsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.updateAcademic(request)));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile photo")
    public ResponseEntity<ApiResponse<StudentDto.StudentProfileResponse>> uploadPhoto(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.uploadProfilePhoto(file)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete student account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        studentService.deleteAccount();
        return ResponseEntity.ok(ApiResponse.ok("Account deleted", null));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody Map<String, String> body) {
        studentService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
