package com.clause.app.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SchemaValidator {

    private static final int MAX_RISK_REASON_LENGTH = 300;
    private static final int MAX_SUGGESTION_LENGTH = 200;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_KEY_POINT_LENGTH = 200;

    private final ObjectMapper objectMapper;

    public SchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidationResult validate(JsonNode root) {
        List<String> errors = new ArrayList<>();

        // overall_summary 검증
        if (!root.has("overall_summary")) {
            errors.add("Missing 'overall_summary'");
        } else {
            JsonNode summary = root.get("overall_summary");
            if (!summary.has("warning_count") || !summary.has("check_count") || !summary.has("ok_count")) {
                errors.add("Missing count fields in overall_summary");
            }
            if (summary.has("key_points") && summary.get("key_points").isArray()) {
                summary.get("key_points").forEach(point -> {
                    if (point.asText().length() > MAX_KEY_POINT_LENGTH) {
                        errors.add("key_point exceeds max length");
                    }
                });
            }
        }

        // items 검증
        if (!root.has("items") || !root.get("items").isArray()) {
            errors.add("Missing or invalid 'items' array");
        } else {
            root.get("items").forEach(item -> {
                if (!item.has("clause_id")) errors.add("Item missing clause_id");
                if (!item.has("title")) errors.add("Item missing title");
                if (!item.has("label")) errors.add("Item missing label");
                if (!item.has("risk_reason")) errors.add("Item missing risk_reason");
                if (!item.has("what_to_confirm")) errors.add("Item missing what_to_confirm");
                if (!item.has("soft_suggestion")) errors.add("Item missing soft_suggestion");
                if (!item.has("triggers")) errors.add("Item missing triggers");

                // label 값 검증
                if (item.has("label")) {
                    String label = item.get("label").asText();
                    if (!label.equals("WARNING") && !label.equals("CHECK") && !label.equals("OK")) {
                        errors.add("Invalid label: " + label);
                    }
                }

                // 길이 제한 검증
                if (item.has("risk_reason")) {
                    String reason = item.get("risk_reason").asText();
                    if (reason.length() > MAX_RISK_REASON_LENGTH) {
                        errors.add("risk_reason exceeds max length");
                    }
                }
                if (item.has("title")) {
                    String title = item.get("title").asText();
                    if (title.length() > MAX_TITLE_LENGTH) {
                        errors.add("title exceeds max length");
                    }
                }
            });
        }

        // negotiation_suggestions 검증
        if (!root.has("negotiation_suggestions") || !root.get("negotiation_suggestions").isArray()) {
            errors.add("Missing or invalid 'negotiation_suggestions'");
        } else {
            root.get("negotiation_suggestions").forEach(suggestion -> {
                if (suggestion.asText().length() > MAX_SUGGESTION_LENGTH) {
                    errors.add("negotiation_suggestion exceeds max length");
                }
            });
        }

        // disclaimer 검증
        if (!root.has("disclaimer")) {
            errors.add("Missing 'disclaimer'");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public JsonNode sanitize(JsonNode root) {
        // 길이 제한 초과 시 자르기
        if (root.has("items") && root.get("items").isArray()) {
            root.get("items").forEach(item -> {
                if (item.has("risk_reason")) {
                    String reason = item.get("risk_reason").asText();
                    if (reason.length() > MAX_RISK_REASON_LENGTH) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) item)
                                .put("risk_reason", reason.substring(0, MAX_RISK_REASON_LENGTH));
                    }
                }
                if (item.has("title")) {
                    String title = item.get("title").asText();
                    if (title.length() > MAX_TITLE_LENGTH) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) item)
                                .put("title", title.substring(0, MAX_TITLE_LENGTH));
                    }
                }
            });
        }

        return root;
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}

