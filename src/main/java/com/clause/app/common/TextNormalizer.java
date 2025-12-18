package com.clause.app.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TextNormalizer {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s{2,}");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern PAGE_NUMBER = Pattern.compile("^\\s*(?:\\d+/\\d+|Page\\s+\\d+|페이지\\s*\\d+)\\s*$", Pattern.MULTILINE);
    private static final Pattern HEADER_FOOTER = Pattern.compile("^(?:목차|차례|Table of Contents|INDEX).*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = text;

        // 페이지 번호 제거
        normalized = PAGE_NUMBER.matcher(normalized).replaceAll("");

        // 헤더/푸터 패턴 제거 (간단한 휴리스틱)
        normalized = HEADER_FOOTER.matcher(normalized).replaceAll("");

        // 반복되는 짧은 헤더 제거 (같은 라인이 여러 페이지 반복)
        normalized = removeRepeatingHeaders(normalized);

        // 연속된 공백 정리
        normalized = MULTIPLE_SPACES.matcher(normalized).replaceAll(" ");

        // 연속된 개행 정리 (최대 2개)
        normalized = MULTIPLE_NEWLINES.matcher(normalized).replaceAll("\n\n");

        // 특수문자 표준화
        normalized = normalized.replaceAll("[\\u00A0]", " "); // Non-breaking space
        normalized = normalized.replaceAll("[\\u200B]", ""); // Zero-width space
        normalized = normalized.replaceAll("[\\uFEFF]", ""); // BOM

        // 앞뒤 공백 제거
        normalized = normalized.trim();

        return normalized;
    }

    private String removeRepeatingHeaders(String text) {
        String[] lines = text.split("\n");
        Map<String, Integer> lineCounts = new HashMap<>();

        // 각 라인의 빈도 계산
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 0 && trimmed.length() < 50) {
                lineCounts.put(trimmed, lineCounts.getOrDefault(trimmed, 0) + 1);
            }
        }

        // 3번 이상 반복되는 짧은 라인 제거
        Set<String> toRemove = lineCounts.entrySet().stream()
                .filter(e -> e.getValue() >= 3 && e.getKey().length() < 50)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (toRemove.isEmpty()) {
            return text;
        }

        return Arrays.stream(lines)
                .filter(line -> !toRemove.contains(line.trim()))
                .collect(Collectors.joining("\n"));
    }
}

