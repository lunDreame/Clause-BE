package com.clause.app.domain.document.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface TextExtractionService {
    String extractText(MultipartFile file) throws Exception;
    String extractText(InputStream inputStream, String contentType) throws Exception;
    boolean supports(String contentType);
}

