package com.clause.app.web;

import com.clause.app.common.ApiResponse;
import com.clause.app.common.RateLimitGuard;
import com.clause.app.domain.analysis.dto.AnalysisRequest;
import com.clause.app.domain.analysis.dto.AnalysisResponse;
import com.clause.app.domain.analysis.entity.AnalysisResult;
import com.clause.app.domain.analysis.service.AnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final RateLimitGuard rateLimitGuard;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ApiResponse<AnalysisResponse> analyze(
            @Valid @RequestBody AnalysisRequest request,
            HttpServletRequest httpRequest) {
        rateLimitGuard.check(getClientIdentifier(httpRequest));

        AnalysisResult result = analysisService.analyze(request);
        AnalysisResponse response = convertToResponse(result);

        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<AnalysisResponse> getAnalysis(
            @PathVariable UUID id,
            HttpServletRequest request) {
        rateLimitGuard.check(getClientIdentifier(request));

        AnalysisResult result = analysisService.getAnalysis(id);
        AnalysisResponse response = convertToResponse(result);

        return ApiResponse.success(response);
    }

    @GetMapping("/documents/{documentId}")
    public ApiResponse<List<AnalysisResponse>> getAnalysesByDocument(
            @PathVariable UUID documentId,
            HttpServletRequest request) {
        rateLimitGuard.check(getClientIdentifier(request));

        List<AnalysisResult> results = analysisService.getAnalysesByDocument(documentId);
        List<AnalysisResponse> responses = results.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    private AnalysisResponse convertToResponse(AnalysisResult result) {
        try {
            AnalysisResponse.AnalysisResponseBuilder builder = AnalysisResponse.builder()
                    .analysisId(result.getId())
                    .disclaimer(result.getDisclaimer());

            // overall_summary 파싱
            if (result.getOverallSummaryJson() != null) {
                JsonNode summaryNode = objectMapper.readTree(result.getOverallSummaryJson());
                AnalysisResponse.OverallSummary summary = AnalysisResponse.OverallSummary.builder()
                        .warningCount(summaryNode.has("warning_count") ? summaryNode.get("warning_count").asInt() : 0)
                        .checkCount(summaryNode.has("check_count") ? summaryNode.get("check_count").asInt() : 0)
                        .okCount(summaryNode.has("ok_count") ? summaryNode.get("ok_count").asInt() : 0)
                        .keyPoints(summaryNode.has("key_points") && summaryNode.get("key_points").isArray() ?
                                objectMapper.convertValue(summaryNode.get("key_points"), List.class) : List.of())
                        .build();
                builder.overallSummary(summary);
            }

            // items 파싱
            if (result.getItemsJson() != null) {
                JsonNode itemsNode = objectMapper.readTree(result.getItemsJson());
                List<AnalysisResponse.AnalysisItem> items = objectMapper.convertValue(itemsNode, List.class);
                builder.items(items);
            }

            // negotiation_suggestions 파싱
            if (result.getNegotiationSuggestionsJson() != null) {
                JsonNode suggestionsNode = objectMapper.readTree(result.getNegotiationSuggestionsJson());
                List<String> suggestions = objectMapper.convertValue(suggestionsNode, List.class);
                builder.negotiationSuggestions(suggestions);
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to convert AnalysisResult to Response", e);
            throw new RuntimeException("Failed to convert analysis result", e);
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

