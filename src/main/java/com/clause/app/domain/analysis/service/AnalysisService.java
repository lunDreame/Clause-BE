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
        // 1) Document 조회
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ClauseException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 2) 텍스트 추출 (없으면 시도)
        if (document.getExtractedText() == null) {
            document = documentService.extractText(document.getId());
        }

        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            throw new ClauseException(ErrorCode.EXTRACTION_FAILED, "추출된 텍스트가 없습니다.");
        }

        // 3) 정규화
        String normalizedText = textNormalizer.normalize(extractedText);

        // 4) PII 마스킹
        String maskedText = maskingUtil.maskForLlm(normalizedText);

        // 5) 조항 세그먼트 생성
        List<ClauseCandidate> segments = clauseSegmenter.segment(maskedText);

        // 6) 룰 엔진 실행
        ContractType contractType = ContractType.valueOf(request.getContractType());
        RuleRunResult ruleResult = ruleEngine.runRules(maskedText, contractType, segments);

        // 7) 후보 조항 선정
        List<ClauseCandidate> topCandidates = ruleEngine.selectTopCandidates(
                ruleResult.getCandidates(), 10, contractType);

        // 8) LLM 호출
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

            // 9) JSON 복구
            String repairedJson = jsonRepairUtil.extractAndRepair(rawJson);
            JsonNode root = objectMapper.readTree(repairedJson);

            // 10) 스키마 검증
            SchemaValidator.ValidationResult validation = schemaValidator.validate(root);
            if (!validation.valid()) {
                log.warn("Schema validation failed: {}", validation.errors());
                root = schemaValidator.sanitize(root);
            }

            // 11) 금지어 가드
            root = forbiddenPhraseGuard.guard(root);

            // 12) 후처리 (서버에서 재계산)
            root = postProcess(root, topCandidates);

            // 13) 저장
            analysisResult.setOverallSummaryJson(objectMapper.writeValueAsString(root.get("overall_summary")));
            analysisResult.setItemsJson(objectMapper.writeValueAsString(root.get("items")));
            analysisResult.setNegotiationSuggestionsJson(objectMapper.writeValueAsString(root.get("negotiation_suggestions")));
            analysisResult.setDisclaimer(root.get("disclaimer").asText());
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

    private JsonNode postProcess(JsonNode root, List<ClauseCandidate> candidates) {
        // overall_summary count 재계산
        JsonNode items = root.get("items");
        if (items != null && items.isArray()) {
            int warningCount = 0;
            int checkCount = 0;
            int okCount = 0;

            for (JsonNode item : items) {
                String label = item.get("label").asText();
                if ("WARNING".equals(label)) warningCount++;
                else if ("CHECK".equals(label)) checkCount++;
                else if ("OK".equals(label)) okCount++;
            }

            com.fasterxml.jackson.databind.node.ObjectNode summary =
                    (com.fasterxml.jackson.databind.node.ObjectNode) root.get("overall_summary");
            summary.put("warning_count", warningCount);
            summary.put("check_count", checkCount);
            summary.put("ok_count", okCount);
        }

        // disclaimer 고정
        com.fasterxml.jackson.databind.node.ObjectNode rootNode =
                (com.fasterxml.jackson.databind.node.ObjectNode) root;
        rootNode.put("disclaimer", "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다.");

        return root;
    }
}

