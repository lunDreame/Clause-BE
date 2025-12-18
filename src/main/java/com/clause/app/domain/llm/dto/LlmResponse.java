package com.clause.app.domain.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    private String content;
    private String model;
    private Integer usageTokens;
    private Integer promptTokens;
    private Integer completionTokens;
}

