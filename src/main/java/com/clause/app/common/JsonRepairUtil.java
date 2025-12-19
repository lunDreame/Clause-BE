package com.clause.app.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsonRepairUtil {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
    private static final Pattern SMART_QUOTES = Pattern.compile("[\u201C\u201D\u2018\u2019]");
    
    private final ObjectMapper objectMapper;

    public JsonRepairUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractAndRepair(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Raw text is empty");
        }

        String jsonText = rawText.trim();

        Matcher blockMatcher = JSON_BLOCK.matcher(jsonText);
        if (blockMatcher.find()) {
            jsonText = blockMatcher.group(1).trim();
        }

        Matcher objectMatcher = JSON_OBJECT.matcher(jsonText);
        if (objectMatcher.find()) {
            jsonText = objectMatcher.group();
        } else {
            throw new IllegalArgumentException("No JSON object found");
        }

        jsonText = SMART_QUOTES.matcher(jsonText).replaceAll("\"");
        jsonText = removeTrailingCommas(jsonText);

        try {
            JsonNode node = objectMapper.readTree(jsonText);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private String removeTrailingCommas(String json) {
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");
        return json;
    }
}

