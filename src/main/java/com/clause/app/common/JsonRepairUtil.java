package com.clause.app.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JsonRepairUtil {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
    private static final Pattern SMART_QUOTES = Pattern.compile("[\u201C\u201D\u2018\u2019]"); // Smart quotes: ""''
    
    private final ObjectMapper objectMapper;

    public JsonRepairUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractAndRepair(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Raw text is empty");
        }

        String jsonText = rawText.trim();

        // 1. ```json ... ``` 또는 ``` ... ``` 제거
        Matcher blockMatcher = JSON_BLOCK.matcher(jsonText);
        if (blockMatcher.find()) {
            jsonText = blockMatcher.group(1).trim();
        }

        // 2. 첫 '{' 부터 마지막 '}'까지 추출
        Matcher objectMatcher = JSON_OBJECT.matcher(jsonText);
        if (objectMatcher.find()) {
            jsonText = objectMatcher.group();
        } else {
            throw new IllegalArgumentException("No JSON object found");
        }

        // 3. Smart quotes를 일반 quotes로 변경
        jsonText = SMART_QUOTES.matcher(jsonText).replaceAll("\"");

        // 4. Trailing comma 제거 (간단한 휴리스틱)
        jsonText = removeTrailingCommas(jsonText);

        // 5. JSON 파싱 시도
        try {
            JsonNode node = objectMapper.readTree(jsonText);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("JSON parsing failed, attempting repair: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    private String removeTrailingCommas(String json) {
        // 배열/객체 내부의 trailing comma 제거
        json = json.replaceAll(",\\s*}", "}");
        json = json.replaceAll(",\\s*]", "]");
        return json;
    }

    public boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

