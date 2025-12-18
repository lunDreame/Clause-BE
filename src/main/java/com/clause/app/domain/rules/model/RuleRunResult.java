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
public class RuleRunResult {
    private List<ClauseCandidate> candidates;
    private Map<String, Integer> categoryScores;
    private int totalTriggers;
}

