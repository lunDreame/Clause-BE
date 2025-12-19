package com.clause.app.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisResponse {
    private UUID analysisId;
    private OverallSummary overallSummary;
    private List<AnalysisItem> items;
    private List<String> negotiationSuggestions;
    private String disclaimer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallSummary {
        private Integer warningCount;
        private Integer checkCount;
        private Integer okCount;
        private List<String> keyPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisItem {
        @JsonProperty("clause_id")
        private String clauseId;
        private String title;
        private String label; // WARNING, CHECK, OK
        @JsonProperty("risk_reason")
        private String riskReason;
        @JsonProperty("what_to_confirm")
        private List<String> whatToConfirm;
        @JsonProperty("soft_suggestion")
        private List<String> softSuggestion;
        private List<String> triggers;
    }
}

