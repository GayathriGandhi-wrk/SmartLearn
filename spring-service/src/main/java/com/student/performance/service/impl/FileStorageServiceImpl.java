package com.student.performance.service.impl;

import com.student.performance.config.AppProperties;
import com.student.performance.exception.BadRequestException;
import com.student.performance.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "application/pdf", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Path baseDir;

    public FileStorageServiceImpl(AppProperties properties) {
        this.baseDir = Paths.get(properties.getUpload().getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create upload directory", ex);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 5 MB limit");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if ("profiles".equals(subDir) && !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Only image files (JPEG, PNG, GIF, WEBP) are allowed");
        }
        if ("documents".equals(subDir) && !ALLOWED_DOC_TYPES.contains(contentType)) {
            throw new BadRequestException("File type not allowed");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;

        Path targetDir = baseDir.resolve(subDir).normalize();
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();
            if (!target.startsWith(baseDir)) {
                throw new BadRequestException("Invalid file path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return subDir + "/" + filename;
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store file: " + ex.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Path target = baseDir.resolve(path).normalize();
            if (target.startsWith(baseDir)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }
}
