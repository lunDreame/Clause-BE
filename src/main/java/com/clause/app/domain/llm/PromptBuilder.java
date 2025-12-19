package com.clause.app.domain.llm;

import com.clause.app.domain.rules.enums.ContractType;
import com.clause.app.domain.rules.enums.UserProfile;
import com.clause.app.domain.rules.model.ClauseCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
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

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildDeveloperPrompt(ContractType contractType, UserProfile userProfile, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 계약서 조항을 분석하는 전문가입니다. 아래 계약 조항들을 분석하여 JSON 형식으로 결과를 반환하세요.\n\n");
        
        sb.append("## 분석 컨텍스트\n");
        sb.append("- 계약 유형: ").append(contractType.name()).append("\n");
        sb.append("- 사용자 프로필: ").append(userProfile.name()).append("\n");
        sb.append("- 언어: ").append(language).append("\n\n");

        sb.append("## 분석 작업\n");
        sb.append("1. 각 조항의 위험도를 평가하고 적절한 라벨을 부여하세요\n");
        sb.append("2. 각 조항에 대한 위험 이유를 설명하세요 (1~2문장, 단정적 표현 금지)\n");
        sb.append("3. 확인이 필요한 사항들을 나열하세요 (1~4개)\n");
        sb.append("4. 완곡한 표현으로 제안사항을 작성하세요 (1~3개, 질문형 권장)\n");
        sb.append("5. 전체 계약서에 대한 협상 제안을 작성하세요\n");
        sb.append("6. 핵심 포인트를 요약하세요\n\n");

        sb.append("## 라벨 정의\n");
        sb.append("- WARNING: 일반적으로 분쟁이 잦거나, 개인에게 불리할 수 있는 조항\n");
        sb.append("- CHECK: 수치/기간/범위/조건 등 맥락 확인이 필요한 조항\n");
        sb.append("- OK: 상대적으로 표준적이거나 위험이 낮은 조항\n\n");

        sb.append("## 필수 JSON 구조 (모든 필드가 반드시 포함되어야 함)\n");
        sb.append("{\n");
        sb.append("  \"overall_summary\": {\n");
        sb.append("    \"warning_count\": <숫자>,\n");
        sb.append("    \"check_count\": <숫자>,\n");
        sb.append("    \"ok_count\": <숫자>,\n");
        sb.append("    \"key_points\": [<문자열>, ...]\n");
        sb.append("  },\n");
        sb.append("  \"items\": [\n");
        sb.append("    {\n");
        sb.append("      \"clause_id\": <문자열>,\n");
        sb.append("      \"title\": <문자열>,\n");
        sb.append("      \"label\": \"WARNING\" | \"CHECK\" | \"OK\",\n");
        sb.append("      \"risk_reason\": <문자열>,\n");
        sb.append("      \"what_to_confirm\": [<문자열>, ...],\n");
        sb.append("      \"soft_suggestion\": [<문자열>, ...],\n");
        sb.append("      \"triggers\": [<문자열>, ...]\n");
        sb.append("    }, ...\n");
        sb.append("  ],\n");
        sb.append("  \"negotiation_suggestions\": [<문자열>, ...],\n");
        sb.append("  \"disclaimer\": \"").append(DISCLAIMER).append("\"\n");
        sb.append("}\n\n");

        sb.append("## 필수 요구사항\n");
        sb.append("- overall_summary: 필수. warning_count, check_count, ok_count, key_points를 반드시 포함\n");
        sb.append("- items: 필수. 조항 분석 항목 배열. 빈 배열이면 안 됨\n");
        sb.append("- negotiation_suggestions: 필수. 문자열 배열. 빈 배열이면 안 됨\n");
        sb.append("- disclaimer: 필수. 반드시 정확히 \"").append(DISCLAIMER).append("\"로 설정\n");
        sb.append("- risk_reason: 1~2문장, 한국어, 단정적 표현 금지 (예: '~할 수 있어요', '~일 수 있어요')\n");
        sb.append("- what_to_confirm: 1~4개 항목\n");
        sb.append("- soft_suggestion: 1~3개 항목 (질문형이나 완곡한 표현 사용)\n");
        sb.append("- triggers: 입력된 rule_triggers 값을 그대로 포함\n");

        return sb.toString();
    }

    public String buildUserPrompt(List<ClauseCandidate> candidates, ContractType contractType, UserProfile userProfile, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 분석할 계약 조항들\n\n");
        
        for (int i = 0; i < candidates.size(); i++) {
            ClauseCandidate candidate = candidates.get(i);
            sb.append("### 조항 ").append(i + 1).append("\n");
            sb.append("- ID: ").append(candidate.getId()).append("\n");
            sb.append("- 제목: ").append(candidate.getTitle()).append("\n");
            sb.append("- 조항 내용:\n");
            sb.append(candidate.getText()).append("\n\n");
            
            if (!candidate.getRuleTriggers().isEmpty()) {
                sb.append("- 감지된 규칙 트리거: ");
                String triggers = candidate.getRuleTriggers().stream()
                        .map(t -> t.getCategory().name())
                        .collect(java.util.stream.Collectors.joining(", "));
                sb.append(triggers).append("\n\n");
            }
        }
        
        sb.append("위 조항들을 분석하여 JSON 형식으로 결과를 반환하세요. ");
        sb.append("각 조항에 대해 clause_id는 입력된 ID를 그대로 사용하고, ");
        sb.append("triggers 필드에는 해당 조항의 rule_triggers 값을 그대로 포함하세요.");

        return sb.toString();
    }
}

