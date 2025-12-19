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

                if (item.has("label")) {
                    String label = item.get("label").asText();
                    if (!label.equals("WARNING") && !label.equals("CHECK") && !label.equals("OK")) {
                        errors.add("Invalid label: " + label);
                    }
                }

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

        if (!root.has("negotiation_suggestions") || !root.get("negotiation_suggestions").isArray()) {
            errors.add("Missing or invalid 'negotiation_suggestions'");
        } else {
            root.get("negotiation_suggestions").forEach(suggestion -> {
                if (suggestion.asText().length() > MAX_SUGGESTION_LENGTH) {
                    errors.add("negotiation_suggestion exceeds max length");
                }
            });
        }

        if (!root.has("disclaimer")) {
            errors.add("Missing 'disclaimer'");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public JsonNode sanitize(JsonNode root) {
        com.fasterxml.jackson.databind.node.ObjectNode rootNode;
        if (root.isObject()) {
            rootNode = (com.fasterxml.jackson.databind.node.ObjectNode) root;
        } else {
            rootNode = objectMapper.createObjectNode();
            root.fields().forEachRemaining(entry -> rootNode.set(entry.getKey(), entry.getValue()));
        }

        if (!rootNode.has("overall_summary")) {
            com.fasterxml.jackson.databind.node.ObjectNode summary = 
                    objectMapper.createObjectNode();
            summary.put("warning_count", 0);
            summary.put("check_count", 0);
            summary.put("ok_count", 0);
            summary.set("key_points", objectMapper.createArrayNode());
            rootNode.set("overall_summary", summary);
        } else {
            JsonNode summaryNode = rootNode.get("overall_summary");
            com.fasterxml.jackson.databind.node.ObjectNode summary;
            if (summaryNode.isObject()) {
                summary = (com.fasterxml.jackson.databind.node.ObjectNode) summaryNode;
            } else {
                summary = objectMapper.createObjectNode();
                rootNode.set("overall_summary", summary);
            }
            if (!summary.has("warning_count")) summary.put("warning_count", 0);
            if (!summary.has("check_count")) summary.put("check_count", 0);
            if (!summary.has("ok_count")) summary.put("ok_count", 0);
            if (!summary.has("key_points")) summary.set("key_points", objectMapper.createArrayNode());
        }

        if (!rootNode.has("items") || !rootNode.get("items").isArray()) {
            rootNode.set("items", objectMapper.createArrayNode());
        } else {
            com.fasterxml.jackson.databind.node.ArrayNode itemsArray = 
                    (com.fasterxml.jackson.databind.node.ArrayNode) rootNode.get("items");
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonNode item = itemsArray.get(i);
                com.fasterxml.jackson.databind.node.ObjectNode itemNode;
                if (item.isObject()) {
                    itemNode = (com.fasterxml.jackson.databind.node.ObjectNode) item;
                } else {
                    itemNode = objectMapper.createObjectNode();
                    itemsArray.set(i, itemNode);
                }
                
                if (!item.has("clause_id")) itemNode.put("clause_id", "");
                if (!item.has("title")) itemNode.put("title", "");
                if (!item.has("label")) itemNode.put("label", "OK");
                if (!item.has("risk_reason")) itemNode.put("risk_reason", "");
                if (!item.has("what_to_confirm")) itemNode.set("what_to_confirm", objectMapper.createArrayNode());
                if (!item.has("soft_suggestion")) itemNode.set("soft_suggestion", objectMapper.createArrayNode());
                if (!item.has("triggers")) itemNode.set("triggers", objectMapper.createArrayNode());
                
                if (item.has("label")) {
                    String label = item.get("label").asText();
                    if (!label.equals("WARNING") && !label.equals("CHECK") && !label.equals("OK")) {
                        itemNode.put("label", "OK");
                    }
                }
                
                if (itemNode.has("risk_reason")) {
                    String reason = itemNode.get("risk_reason").asText();
                    if (reason.length() > MAX_RISK_REASON_LENGTH) {
                        itemNode.put("risk_reason", reason.substring(0, MAX_RISK_REASON_LENGTH));
                    }
                }
                if (itemNode.has("title")) {
                    String title = itemNode.get("title").asText();
                    if (title.length() > MAX_TITLE_LENGTH) {
                        itemNode.put("title", title.substring(0, MAX_TITLE_LENGTH));
                    }
                }
            }
        }

        if (!rootNode.has("negotiation_suggestions") || !rootNode.get("negotiation_suggestions").isArray()) {
            rootNode.set("negotiation_suggestions", objectMapper.createArrayNode());
        } else {
            com.fasterxml.jackson.databind.node.ArrayNode suggestions = 
                    (com.fasterxml.jackson.databind.node.ArrayNode) rootNode.get("negotiation_suggestions");
            for (int i = 0; i < suggestions.size(); i++) {
                String suggestion = suggestions.get(i).asText();
                if (suggestion.length() > MAX_SUGGESTION_LENGTH) {
                    suggestions.set(i, objectMapper.valueToTree(suggestion.substring(0, MAX_SUGGESTION_LENGTH)));
                }
            }
        }

        if (!rootNode.has("disclaimer")) {
            rootNode.put("disclaimer", "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다.");
        }

        return rootNode;
    }

    public record ValidationResult(boolean valid, List<String> errors) {}
}

