package com.clause.app.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class ForbiddenPhraseGuard {

    private static final Set<String> FORBIDDEN_PHRASES = new HashSet<>(Arrays.asList(
            "불법", "위법", "무효", "반드시", "확실히", "100%", "절대", "무조건",
            "틀림없이", "확정적으로", "원천적으로", "법적으로", "위법", "무효"
    ));

    private final ObjectMapper objectMapper;

    public ForbiddenPhraseGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode guard(JsonNode root) {
        ObjectNode result = root.deepCopy();

        boolean foundForbidden = false;

        if (result.has("overall_summary")) {
            ObjectNode summary = (ObjectNode) result.get("overall_summary");
            if (summary.has("key_points") && summary.get("key_points").isArray()) {
                ArrayNode keyPoints = (ArrayNode) summary.get("key_points");
                for (int i = 0; i < keyPoints.size(); i++) {
                    String point = keyPoints.get(i).asText();
                    if (containsForbiddenPhrase(point)) {
                        keyPoints.set(i, objectMapper.valueToTree("추가 확인이 필요한 부분이 있어요."));
                        foundForbidden = true;
                    }
                }
            }
        }

        if (result.has("items") && result.get("items").isArray()) {
            ArrayNode items = (ArrayNode) result.get("items");
            for (JsonNode item : items) {
                ObjectNode itemNode = (ObjectNode) item;
                boolean itemModified = false;

                if (itemNode.has("title")) {
                    String title = itemNode.get("title").asText();
                    if (containsForbiddenPhrase(title)) {
                        itemNode.put("title", "추가 확인이 필요한 조항");
                        itemModified = true;
                    }
                }

                if (itemNode.has("risk_reason")) {
                    String reason = itemNode.get("risk_reason").asText();
                    if (containsForbiddenPhrase(reason)) {
                        itemNode.put("risk_reason", "추가 확인이 필요해요.");
                        itemModified = true;
                    }
                }

                if (itemNode.has("soft_suggestion") && itemNode.get("soft_suggestion").isArray()) {
                    ArrayNode suggestions = (ArrayNode) itemNode.get("soft_suggestion");
                    for (int i = 0; i < suggestions.size(); i++) {
                        String suggestion = suggestions.get(i).asText();
                        if (containsForbiddenPhrase(suggestion)) {
                            suggestions.set(i, objectMapper.valueToTree("일반적으로 주의가 필요할 수 있어요."));
                            itemModified = true;
                        }
                    }
                }

                if (itemModified && itemNode.has("label") && itemNode.get("label").asText().equals("WARNING")) {
                    itemNode.put("label", "CHECK");
                }

                if (itemModified) {
                    if (!itemNode.has("triggers") || !itemNode.get("triggers").isArray()) {
                        itemNode.set("triggers", objectMapper.createArrayNode());
                    }
                    ArrayNode triggers = (ArrayNode) itemNode.get("triggers");
                    boolean hasForbidden = false;
                    for (JsonNode trigger : triggers) {
                        if (trigger.asText().equals("FORBIDDEN_PHRASE")) {
                            hasForbidden = true;
                            break;
                        }
                    }
                    if (!hasForbidden) {
                        triggers.add("FORBIDDEN_PHRASE");
                    }
                    foundForbidden = true;
                }
            }
        }

        if (result.has("negotiation_suggestions") && result.get("negotiation_suggestions").isArray()) {
            ArrayNode suggestions = (ArrayNode) result.get("negotiation_suggestions");
            for (int i = 0; i < suggestions.size(); i++) {
                String suggestion = suggestions.get(i).asText();
                if (containsForbiddenPhrase(suggestion)) {
                    suggestions.set(i, objectMapper.valueToTree("일반적으로 주의가 필요할 수 있어요."));
                    foundForbidden = true;
                }
            }
        }

        return result;
    }

    private boolean containsForbiddenPhrase(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        return FORBIDDEN_PHRASES.stream().anyMatch(lowerText::contains);
    }
}

