package com.clause.app.domain.analysis.service;

import com.clause.app.common.*;
import com.clause.app.domain.analysis.dto.AnalysisRequest;
import com.clause.app.domain.analysis.dto.AnalysisResponse;
import com.clause.app.domain.analysis.entity.AnalysisResult;
import com.clause.app.domain.analysis.repo.AnalysisRepository;
import com.clause.app.domain.document.entity.Document;
import com.clause.app.domain.document.repo.DocumentRepository;
import com.clause.app.domain.document.service.DocumentService;
import com.clause.app.domain.llm.LlmClient;
import com.clause.app.domain.llm.PromptBuilder;
import com.clause.app.domain.llm.dto.LlmRequest;
import com.clause.app.domain.llm.dto.LlmResponse;
import com.clause.app.domain.rules.engine.ClauseSegmenter;
import com.clause.app.domain.rules.engine.RuleEngine;
import com.clause.app.domain.rules.enums.ContractType;
import com.clause.app.domain.rules.enums.UserProfile;
import com.clause.app.domain.rules.model.ClauseCandidate;
import com.clause.app.domain.rules.model.RuleRunResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final AnalysisRepository analysisRepository;
    private final ClauseSegmenter clauseSegmenter;
    private final RuleEngine ruleEngine;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final TextNormalizer textNormalizer;
    private final MaskingUtil maskingUtil;
    private final JsonRepairUtil jsonRepairUtil;
    private final SchemaValidator schemaValidator;
    private final ForbiddenPhraseGuard forbiddenPhraseGuard;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalysisResult analyze(AnalysisRequest request) {
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ClauseException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getExtractedText() == null) {
            document = documentService.extractText(document.getId());
        }

        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new ClauseException(ErrorCode.EXTRACTION_FAILED, "추출된 텍스트가 없습니다.");
        }

        String normalizedText = textNormalizer.normalize(extractedText);
        String maskedText = maskingUtil.maskForLlm(normalizedText);
        List<ClauseCandidate> segments = clauseSegmenter.segment(maskedText);

        ContractType contractType = ContractType.valueOf(request.getContractType());
        RuleRunResult ruleResult = ruleEngine.runRules(maskedText, contractType, segments);

        List<ClauseCandidate> topCandidates = ruleEngine.selectTopCandidates(
                ruleResult.getCandidates(), 10, contractType);

        UserProfile userProfile = UserProfile.valueOf(request.getUserProfile());
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String developerPrompt = promptBuilder.buildDeveloperPrompt(
                contractType, userProfile, request.getLanguage());
        String userPrompt = promptBuilder.buildUserPrompt(
                topCandidates, contractType, userProfile, request.getLanguage());

        LlmRequest llmRequest = LlmRequest.builder()
                .systemPrompt(systemPrompt)
                .developerPrompt(developerPrompt)
                .userPrompt(userPrompt)
                .model("gpt-4o-mini")
                .temperature(0.3)
                .maxTokens(4000)
                .build();

        AnalysisResult analysisResult = AnalysisResult.builder()
                .documentId(request.getDocumentId())
                .contractType(request.getContractType())
                .userProfile(request.getUserProfile())
                .language(request.getLanguage())
                .status("PENDING")
                .build();

        try {
            LlmResponse llmResponse = llmClient.call(llmRequest);
            String rawJson = llmResponse.getContent();
            if (rawJson == null || rawJson.isBlank()) {
                throw new ClauseException(ErrorCode.JSON_REPAIR_FAILED, "LLM 응답이 비어있습니다.");
            }

            String repairedJson;
            try {
                repairedJson = jsonRepairUtil.extractAndRepair(rawJson);
            } catch (Exception e) {
                log.error("JSON 복구 실패. 원본 응답: {}", rawJson.substring(0, Math.min(500, rawJson.length())), e);
                throw new ClauseException(ErrorCode.JSON_REPAIR_FAILED, "JSON 복구 실패: " + e.getMessage());
            }
            
            JsonNode rootNode = objectMapper.readTree(repairedJson);
            com.fasterxml.jackson.databind.node.ObjectNode root = 
                    rootNode.isObject() ? (com.fasterxml.jackson.databind.node.ObjectNode) rootNode 
                                       : objectMapper.createObjectNode();

            SchemaValidator.ValidationResult validation = schemaValidator.validate(root);
            if (!validation.valid()) {
                log.warn("Schema validation failed: {}", validation.errors());
                root = (com.fasterxml.jackson.databind.node.ObjectNode) schemaValidator.sanitize(root);
            }

            JsonNode guardedRoot = forbiddenPhraseGuard.guard(root);
            if (!guardedRoot.isObject()) {
                guardedRoot = root;
            } else {
                root = (com.fasterxml.jackson.databind.node.ObjectNode) guardedRoot;
            }

            root = postProcess(root, topCandidates);
            JsonNode overallSummary = root.get("overall_summary");
            JsonNode items = root.get("items");
            JsonNode negotiationSuggestions = root.get("negotiation_suggestions");
            JsonNode disclaimer = root.get("disclaimer");
            
            if (overallSummary != null) {
                analysisResult.setOverallSummaryJson(objectMapper.writeValueAsString(overallSummary));
            }
            if (items != null) {
                analysisResult.setItemsJson(objectMapper.writeValueAsString(items));
            }
            if (negotiationSuggestions != null) {
                analysisResult.setNegotiationSuggestionsJson(objectMapper.writeValueAsString(negotiationSuggestions));
            }
            if (disclaimer != null && !disclaimer.isNull()) {
                analysisResult.setDisclaimer(disclaimer.asText());
            }
            analysisResult.setRuleTriggersJson(objectMapper.writeValueAsString(
                    topCandidates.stream()
                            .flatMap(c -> c.getRuleTriggers().stream())
                            .map(t -> t.getCategory().name())
                            .distinct()
                            .collect(Collectors.toList())));
            analysisResult.setLlmModel(llmResponse.getModel());
            analysisResult.setStatus("DONE");

        } catch (ClauseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Analysis failed", e);
            analysisResult.setStatus("FAILED");
            analysisResult.setErrorCode(ErrorCode.JSON_REPAIR_FAILED.name());
        }

        return analysisRepository.save(analysisResult);
    }

    @Transactional(readOnly = true)
    public AnalysisResult getAnalysis(UUID analysisId) {
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ClauseException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<AnalysisResult> getAnalysesByDocument(UUID documentId) {
        return analysisRepository.findByDocumentIdOrderByCreatedAtDesc(
                documentId, org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @Transactional(readOnly = true)
    public List<AnalysisResult> getAnalysisHistory(int page, int size) {
        return analysisRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    private com.fasterxml.jackson.databind.node.ObjectNode postProcess(
            com.fasterxml.jackson.databind.node.ObjectNode root, List<ClauseCandidate> candidates) {
        JsonNode items = root.get("items");
        if (items != null && items.isArray()) {
            int warningCount = 0;
            int checkCount = 0;
            int okCount = 0;

            for (JsonNode item : items) {
                JsonNode labelNode = item.get("label");
                if (labelNode != null && !labelNode.isNull()) {
                    String label = labelNode.asText();
                    if ("WARNING".equals(label)) warningCount++;
                    else if ("CHECK".equals(label)) checkCount++;
                    else if ("OK".equals(label)) okCount++;
                }
            }

            JsonNode summaryNode = root.get("overall_summary");
            com.fasterxml.jackson.databind.node.ObjectNode summary;
            if (summaryNode != null && summaryNode.isObject()) {
                summary = (com.fasterxml.jackson.databind.node.ObjectNode) summaryNode;
            } else {
                summary = objectMapper.createObjectNode();
                root.set("overall_summary", summary);
            }
            summary.put("warning_count", warningCount);
            summary.put("check_count", checkCount);
            summary.put("ok_count", okCount);
        }

        root.put("disclaimer", "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다.");

        return root;
    }
}

