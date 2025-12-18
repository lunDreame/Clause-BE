package com.clause.app.domain.rules.engine;

import com.clause.app.domain.rules.model.ClauseCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ClauseSegmenter {

    // 조항 패턴: 제1조, 제일조, 제이십조 등
    private static final Pattern CLAUSE_PATTERN = Pattern.compile(
            "^\\s*제\\s*(\\d+|[일이삼사오육칠팔구십백천]+)\\s*조\\s*(.*)$",
            Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    private static final int MAX_SEGMENT_LENGTH = 6000;

    public List<ClauseCandidate> segment(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        List<ClauseCandidate> segments = new ArrayList<>();
        Matcher matcher = CLAUSE_PATTERN.matcher(text);

        List<MatchInfo> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(new MatchInfo(matcher.start(), matcher.end(), matcher.group(0)));
        }

        if (matches.isEmpty()) {
            // 조항 패턴이 없으면 문장/단락 기준으로 분리
            return segmentByParagraph(text);
        }

        // 조항별로 세그먼트 생성
        for (int i = 0; i < matches.size(); i++) {
            MatchInfo current = matches.get(i);
            int startIndex = current.start;
            int endIndex = (i < matches.size() - 1) ? matches.get(i + 1).start : text.length();

            String segmentText = text.substring(startIndex, endIndex).trim();
            String title = extractTitle(segmentText);

            // 너무 긴 세그먼트는 분할
            if (segmentText.length() > MAX_SEGMENT_LENGTH) {
                List<ClauseCandidate> subSegments = splitLongSegment(segmentText, title, startIndex);
                segments.addAll(subSegments);
            } else {
                segments.add(ClauseCandidate.builder()
                        .id("C-" + String.format("%03d", segments.size() + 1))
                        .title(title)
                        .text(segmentText)
                        .startIndex(startIndex)
                        .endIndex(endIndex)
                        .build());
            }
        }

        log.info("Segmented text into {} clauses", segments.size());
        return segments;
    }

    private List<ClauseCandidate> segmentByParagraph(String text) {
        List<ClauseCandidate> segments = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");

        for (int i = 0; i < paragraphs.length; i++) {
            String para = paragraphs[i].trim();
            if (para.length() > 50) { // 최소 길이 필터
                segments.add(ClauseCandidate.builder()
                        .id("C-" + String.format("%03d", segments.size() + 1))
                        .title("조항 " + (segments.size() + 1))
                        .text(para)
                        .startIndex(0) // 정확한 인덱스는 복잡하므로 간소화
                        .endIndex(para.length())
                        .build());
            }
        }

        return segments;
    }

    private String extractTitle(String segmentText) {
        Matcher matcher = CLAUSE_PATTERN.matcher(segmentText);
        if (matcher.find()) {
            String fullMatch = matcher.group(0);
            // 제목 부분 추출 (첫 줄)
            String[] lines = segmentText.split("\n");
            if (lines.length > 0) {
                return lines[0].trim();
            }
            return fullMatch;
        }
        return "조항";
    }

    private List<ClauseCandidate> splitLongSegment(String text, String title, int baseIndex) {
        List<ClauseCandidate> subSegments = new ArrayList<>();
        String[] sentences = text.split("[.!?。！？]\\s+");

        StringBuilder currentSegment = new StringBuilder();
        int currentStart = 0;
        int segmentIndex = 0;

        for (String sentence : sentences) {
            if (currentSegment.length() + sentence.length() > MAX_SEGMENT_LENGTH && currentSegment.length() > 0) {
                subSegments.add(ClauseCandidate.builder()
                        .id("C-" + String.format("%03d", segmentIndex++))
                        .title(title + " (부분 " + (subSegments.size() + 1) + ")")
                        .text(currentSegment.toString().trim())
                        .startIndex(baseIndex + currentStart)
                        .endIndex(baseIndex + currentStart + currentSegment.length())
                        .build());
                currentSegment = new StringBuilder();
                currentStart += currentSegment.length();
            }
            currentSegment.append(sentence).append(". ");
        }

        if (currentSegment.length() > 0) {
            subSegments.add(ClauseCandidate.builder()
                    .id("C-" + String.format("%03d", segmentIndex))
                    .title(title + " (부분 " + (subSegments.size() + 1) + ")")
                    .text(currentSegment.toString().trim())
                    .startIndex(baseIndex + currentStart)
                    .endIndex(baseIndex + currentStart + currentSegment.length())
                    .build());
        }

        return subSegments;
    }

    private static class MatchInfo {
        final int start;
        final int end;
        final String text;

        MatchInfo(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }
}

