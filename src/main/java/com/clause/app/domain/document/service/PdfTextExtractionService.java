package com.clause.app.domain.document.service;

import com.clause.app.common.ClauseException;
import com.clause.app.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class PdfTextExtractionService implements TextExtractionService {

    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }

    @Override
    public String extractText(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            return extractText(inputStream, file.getContentType());
        }
    }

    @Override
    public String extractText(InputStream inputStream, String contentType) throws Exception {
        if (!supports(contentType)) {
            throw new IllegalArgumentException("Unsupported content type: " + contentType);
        }

        try {
            byte[] pdfBytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(document.getNumberOfPages());
                return stripper.getText(document);
            }
        } catch (Exception e) {
            log.error("PDF extraction failed", e);
            throw new ClauseException(ErrorCode.EXTRACTION_FAILED, "PDF 텍스트 추출 실패: " + e.getMessage());
        }
    }
}

