package com.student.performance.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String store(MultipartFile file, String subDir);
    void delete(String path);
}
