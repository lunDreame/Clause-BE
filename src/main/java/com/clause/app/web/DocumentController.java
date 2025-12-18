package com.clause.app.web;

import com.clause.app.common.ApiResponse;
import com.clause.app.common.RateLimitGuard;
import com.clause.app.domain.document.dto.DocumentDto;
import com.clause.app.domain.document.entity.Document;
import com.clause.app.domain.document.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final RateLimitGuard rateLimitGuard;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentDto> uploadDocument(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        rateLimitGuard.check(getClientIdentifier(request));

        Document document = documentService.upload(file);
        DocumentDto dto = DocumentDto.builder()
                .documentId(document.getId())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .createdAt(document.getCreatedAt())
                .build();

        return ApiResponse.success(dto);
    }

    @PostMapping("/{id}/extract")
    public ApiResponse<DocumentDto> extractText(
            @PathVariable UUID id,
            HttpServletRequest request) {
        rateLimitGuard.check(getClientIdentifier(request));

        Document document = documentService.extractText(id);
        DocumentDto dto = DocumentDto.builder()
                .documentId(document.getId())
                .textLength(document.getExtractedText() != null ? document.getExtractedText().length() : null)
                .textSha256(document.getTextSha256())
                .build();

        return ApiResponse.success(dto);
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDto> getDocument(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeText,
            HttpServletRequest request) {
        rateLimitGuard.check(getClientIdentifier(request));

        Document document = documentService.getDocument(id);
        DocumentDto.DocumentDtoBuilder builder = DocumentDto.builder()
                .documentId(document.getId())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .createdAt(document.getCreatedAt());

        if (includeText) {
            builder.extractedText(document.getExtractedText())
                    .textLength(document.getExtractedText() != null ? document.getExtractedText().length() : null)
                    .textSha256(document.getTextSha256());
        }

        return ApiResponse.success(builder.build());
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

