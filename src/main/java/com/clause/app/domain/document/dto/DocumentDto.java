package com.clause.app.domain.document.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto {
    private UUID documentId;
    private String originalFileName;
    private String contentType;
    private Long sizeBytes;
    private String extractedText;
    private Integer textLength;
    private String textSha256;
    private Instant createdAt;
}

