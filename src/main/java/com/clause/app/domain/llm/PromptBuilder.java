package com.clause.app.domain.llm;

import com.clause.app.domain.rules.enums.ContractType;
import com.clause.app.domain.rules.enums.UserProfile;
import com.clause.app.domain.rules.model.ClauseCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            OUTPUT ONLY VALID JSON. NO MARKDOWN. NO EXTRA TEXT.
            If you output anything other than JSON, it is considered a failure.
            You are not a lawyer. This is not legal advice.
            Never use these Korean words: 불법, 위법, 무효, 반드시, 확실히, 100%, 절대, 무조건.
            Use only cautious phrasing in Korean.
            Return JSON that EXACTLY matches the schema. No extra keys. No missing keys.
            """;

    private static final String DISCLAIMER = "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다.";

    private final ObjectMapper objectMapper;

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildDeveloperPrompt(ContractType contractType, UserProfile userProfile, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the following contract clauses in Korean.\n\n");
        sb.append("Contract Type: ").append(contractType.name()).append("\n");
        sb.append("User Profile: ").append(userProfile.name()).append("\n");
        sb.append("Language: ").append(language).append("\n\n");

        sb.append("Label definitions:\n");
        sb.append("- WARNING: 일반적으로 분쟁이 잦거나, 개인에게 불리할 수 있는 조항\n");
        sb.append("- CHECK: 수치/기간/범위/조건 등 맥락 확인이 필요한 조항\n");
        sb.append("- OK: 상대적으로 표준적이거나 위험이 낮은 조항\n\n");

        sb.append("Requirements:\n");
        sb.append("- risk_reason: 1~2문장, 한국어, 단정 금지\n");
        sb.append("- what_to_confirm: 1~4개 항목\n");
        sb.append("- soft_suggestion: 1~3개 항목 (질문/완곡 표현)\n");
        sb.append("- triggers: 입력 rule_triggers 그대로 포함\n");
        sb.append("- disclaimer: ").append(DISCLAIMER).append("\n");

        return sb.toString();
    }

    public String buildUserPrompt(List<ClauseCandidate> candidates, ContractType contractType, UserProfile userProfile, String language) {
        try {
            Map<String, Object> userPromptMap = new HashMap<>();
            userPromptMap.put("contract_type", contractType.name());
            userPromptMap.put("user_profile", userProfile.name());
            userPromptMap.put("language", language);

            // Schema
            Map<String, Object> schema = new HashMap<>();
            Map<String, Object> overallSummary = new HashMap<>();
            overallSummary.put("warning_count", "number");
            overallSummary.put("check_count", "number");
            overallSummary.put("ok_count", "number");
            overallSummary.put("key_points", "string[]");
            schema.put("overall_summary", overallSummary);

            Map<String, Object> itemSchema = new HashMap<>();
            itemSchema.put("clause_id", "string");
            itemSchema.put("title", "string");
            itemSchema.put("label", "WARNING|CHECK|OK");
            itemSchema.put("risk_reason", "string");
            itemSchema.put("what_to_confirm", "string[]");
            itemSchema.put("soft_suggestion", "string[]");
            itemSchema.put("triggers", "string[]");
            schema.put("items", new Object[]{itemSchema});
            schema.put("negotiation_suggestions", "string[]");
            schema.put("disclaimer", "string");

            userPromptMap.put("schema", schema);

            // Clause candidates
            List<Map<String, Object>> clauseCandidates = candidates.stream()
                    .map(c -> {
                        Map<String, Object> candidateMap = new HashMap<>();
                        candidateMap.put("id", c.getId());
                        candidateMap.put("title", c.getTitle());
                        candidateMap.put("text", c.getText());
                        candidateMap.put("rule_triggers", c.getRuleTriggers().stream()
                                .map(t -> t.getCategory().name())
                                .toList());
                        candidateMap.put("score", c.getTotalScore());
                        return candidateMap;
                    })
                    .toList();

            userPromptMap.put("clause_candidates", clauseCandidates);

            return objectMapper.writeValueAsString(userPromptMap);
        } catch (Exception e) {
            log.error("Failed to build user prompt", e);
            throw new RuntimeException("Failed to build user prompt", e);
        }
    }
}

