package com.clause.app.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
class ForbiddenPhraseGuardTest {

    @Autowired
    private ForbiddenPhraseGuard guard;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testForbiddenPhraseDetection() throws Exception {
        String json = """
                {
                  "overall_summary": {
                    "warning_count": 1,
                    "check_count": 0,
                    "ok_count": 0,
                    "key_points": ["이 조항은 불법입니다."]
                  },
                  "items": [{
                    "clause_id": "C-001",
                    "title": "제8조",
                    "label": "WARNING",
                    "risk_reason": "이것은 반드시 위법입니다.",
                    "what_to_confirm": ["확인사항"],
                    "soft_suggestion": ["절대 하지 마세요"],
                    "triggers": ["TEST"]
                  }],
                  "negotiation_suggestions": ["확실히 조정하세요"],
                  "disclaimer": "면책"
                }
                """;

        JsonNode root = objectMapper.readTree(json);
        JsonNode guarded = guard.guard(root);

        String keyPoint = guarded.get("overall_summary").get("key_points").get(0).asText();
        assertThat(keyPoint).doesNotContain("불법");

        String label = guarded.get("items").get(0).get("label").asText();
        assertThat(label).isEqualTo("CHECK");

        JsonNode triggers = guarded.get("items").get(0).get("triggers");
        boolean hasForbidden = false;
        for (JsonNode trigger : triggers) {
            if ("FORBIDDEN_PHRASE".equals(trigger.asText())) {
                hasForbidden = true;
                break;
            }
        }
        assertThat(hasForbidden).isTrue();
    }
}

