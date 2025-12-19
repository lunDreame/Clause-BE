package com.clause.app.domain.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    @NotNull(message = "documentId는 필수입니다.")
    private UUID documentId;

    @NotBlank(message = "contractType은 필수입니다.")
    private String contractType; // FREELANCER, EMPLOYMENT, PART_TIME, LEASE, NDA, OTHER

    @NotBlank(message = "userProfile은 필수입니다.")
    private String userProfile; // STUDENT, ENTRY_LEVEL, FREELANCER, INDIVIDUAL_BUSINESS, GENERAL_CONSUMER

    @NotBlank(message = "language는 필수입니다.")
    private String language; // ko-KR
}

