package com.clause.app.domain.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    private String systemPrompt;
    private String developerPrompt;
    private String userPrompt;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}

