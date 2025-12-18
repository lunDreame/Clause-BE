package com.clause.app.domain.document.service;

import com.clause.app.common.ClauseException;
import com.clause.app.common.ErrorCode;
import com.clause.app.common.TextNormalizer;
import com.clause.app.domain.document.entity.Document;
import com.clause.app.domain.document.repo.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final List<TextExtractionService> extractionServices;
    private final List<OcrService> ocrServices;
    private final TextNormalizer textNormalizer;

    @Transactional
    public Document upload(MultipartFile file) {
        // 파일 크기 검증
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new ClauseException(ErrorCode.FILE_TOO_LARGE);
        }

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && !contentType.startsWith("image/"))) {
            throw new ClauseException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }

        try {
            // 파일 저장
            Path storedPath = storageService.store(file);
            String storagePath = storedPath.getFileName().toString();

            // Document 엔티티 생성
            Document document = Document.builder()
                    .originalFileName(file.getOriginalFilename())
                    .contentType(contentType)
                    .sizeBytes(file.getSize())
                    .storagePath(storagePath)
                    .build();

            document = documentRepository.save(document);

            // PDF인 경우 자동으로 텍스트 추출 시도
            if ("application/pdf".equals(contentType)) {
                try {
                    String extractedText = null;
                    for (TextExtractionService service : extractionServices) {
                        if (service.supports(contentType)) {
                            extractedText = service.extractText(file);
                            break;
                        }
                    }
                    if (extractedText != null) {
                        String normalized = textNormalizer.normalize(extractedText);
                        String sha256 = calculateSha256(normalized);
                        document.setExtractedText(normalized);
                        document.setTextSha256(sha256);
                        document = documentRepository.save(document);
                    }
                } catch (Exception e) {
                    log.warn("Failed to auto-extract text during upload", e);
                    // 추출 실패해도 업로드는 성공으로 처리
                }
            }

            log.info("Document uploaded: {}", document.getId());
            return document;
        } catch (ClauseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload document", e);
            throw new ClauseException(ErrorCode.INTERNAL_ERROR, "파일 업로드 실패: " + e.getMessage());
        }
    }

    @Transactional
    public Document extractText(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ClauseException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getExtractedText() != null) {
            return document; // 이미 추출됨
        }

        try {
            String extractedText = null;

            // 저장된 파일 로드
            org.springframework.core.io.Resource resource = storageService.loadAsResource(document.getStoragePath());

            // PDF 텍스트 추출
            if ("application/pdf".equals(document.getContentType())) {
                for (TextExtractionService service : extractionServices) {
                    if (service.supports(document.getContentType())) {
                        try (java.io.InputStream inputStream = resource.getInputStream()) {
                            extractedText = service.extractText(inputStream, document.getContentType());
                            break;
                        }
                    }
                }
            }

            // 이미지 OCR 시도
            if (extractedText == null && document.getContentType() != null && document.getContentType().startsWith("image/")) {
                for (OcrService ocrService : ocrServices) {
                    if (ocrService.supports(document.getContentType())) {
                        throw new ClauseException(ErrorCode.OCR_NOT_IMPLEMENTED);
                    }
                }
            }

            if (extractedText == null) {
                throw new ClauseException(ErrorCode.EXTRACTION_FAILED, "지원하지 않는 파일 형식");
            }

            // 정규화 및 SHA256 계산
            String normalized = textNormalizer.normalize(extractedText);
            String sha256 = calculateSha256(normalized);

            document.setExtractedText(normalized);
            document.setTextSha256(sha256);
            document = documentRepository.save(document);

            log.info("Text extracted for document: {}", documentId);
            return document;
        } catch (ClauseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract text for document: {}", documentId, e);
            throw new ClauseException(ErrorCode.EXTRACTION_FAILED, "텍스트 추출 실패: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Document getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ClauseException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private String calculateSha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA256 calculation failed", e);
        }
    }
}

