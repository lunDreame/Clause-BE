package com.clause.app.domain.document.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.UUID;

public interface StorageService {
    Path store(MultipartFile file) throws Exception;
    Resource loadAsResource(String filename);
    void delete(String filename);
    String generateStoragePath(UUID documentId, String originalFileName);
}

