package com.clause.app.domain.document.service;

import com.clause.app.common.ClauseException;
import com.clause.app.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class NoopOcrService implements OcrService {

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        throw new ClauseException(ErrorCode.OCR_NOT_IMPLEMENTED);
    }
}

