package com.clause.app.domain.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClauseCandidate {
    private String id;
    private String title;
    private String text;
    private int startIndex;
    private int endIndex;
    private List<RuleTrigger> ruleTriggers;
    private int totalScore;
    private Map<String, Integer> categoryScores;
}

