package com.clause.app.domain.document.service;

import com.clause.app.common.ClauseException;
import com.clause.app.common.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    String extractText(MultipartFile file) throws Exception;
    boolean supports(String contentType);
}

