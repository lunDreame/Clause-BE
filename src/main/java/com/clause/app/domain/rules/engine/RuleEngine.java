package com.clause.app.domain.rules.engine;

import com.clause.app.domain.rules.enums.ContractType;
import com.clause.app.domain.rules.enums.RuleCategory;
import com.clause.app.domain.rules.model.ClauseCandidate;
import com.clause.app.domain.rules.model.RulePattern;
import com.clause.app.domain.rules.model.RuleRunResult;
import com.clause.app.domain.rules.model.RuleTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RuleEngine {

    private final RuleCatalogLoader catalogLoader;
    private Map<String, List<CompiledPattern>> compiledPatterns;

    public RuleEngine(RuleCatalogLoader catalogLoader) {
        this.catalogLoader = catalogLoader;
        compilePatterns();
    }

    public RuleRunResult runRules(String text, ContractType contractType, List<ClauseCandidate> candidates) {
        Map<String, Integer> categoryScores = new HashMap<>();
        int totalTriggers = 0;

        for (ClauseCandidate candidate : candidates) {
            List<RuleTrigger> triggers = new ArrayList<>();
            Map<String, Integer> candidateCategoryScores = new HashMap<>();

            for (RulePattern rule : catalogLoader.getRules()) {
                int boost = rule.getBoost() != null ? rule.getBoost().getOrDefault(contractType.name(), 0) : 0;
                int weight = rule.getBaseWeight() + boost;

                List<CompiledPattern> compiled = compiledPatterns.getOrDefault(rule.getId(), Collections.emptyList());
                for (CompiledPattern cp : compiled) {
                    java.util.regex.Matcher matcher = cp.pattern.matcher(candidate.getText());
                    while (matcher.find()) {
                        String matchedText = matcher.group();
                        RuleTrigger trigger = RuleTrigger.builder()
                                .ruleId(rule.getId())
                                .category(rule.getCategory())
                                .severity(rule.getSeverity())
                                .weight(weight)
                                .matchedText(matchedText)
                                .startIndex(matcher.start())
                                .endIndex(matcher.end())
                                .build();
                        triggers.add(trigger);
                        totalTriggers++;

                        // 카테고리 점수 누적
                        candidateCategoryScores.merge(
                                rule.getCategory().name(),
                                weight,
                                Integer::sum
                        );
                        categoryScores.merge(
                                rule.getCategory().name(),
                                weight,
                                Integer::sum
                        );
                    }
                }
            }

            candidate.setRuleTriggers(triggers);
            candidate.setCategoryScores(candidateCategoryScores);
            candidate.setTotalScore(triggers.stream().mapToInt(RuleTrigger::getWeight).sum());
        }

        return RuleRunResult.builder()
                .candidates(candidates)
                .categoryScores(categoryScores)
                .totalTriggers(totalTriggers)
                .build();
    }

    public List<ClauseCandidate> selectTopCandidates(List<ClauseCandidate> candidates, int topN, ContractType contractType) {
        // 점수 기반 정렬
        candidates.sort((a, b) -> {
            int scoreA = a.getTotalScore();
            int scoreB = b.getTotalScore();
            if (scoreA != scoreB) {
                return Integer.compare(scoreB, scoreA);
            }
            // 점수가 같으면 WARNING 우선
            long warningA = a.getRuleTriggers().stream().filter(t -> t.getSeverity().name().equals("WARNING")).count();
            long warningB = b.getRuleTriggers().stream().filter(t -> t.getSeverity().name().equals("WARNING")).count();
            return Long.compare(warningB, warningA);
        });

        // 카테고리 다양성 보정
        List<ClauseCandidate> selected = new ArrayList<>();
        Set<RuleCategory> usedCategories = new HashSet<>();

        // 첫 번째로 점수 높은 것들 선택 (카테고리 고려)
        for (ClauseCandidate candidate : candidates) {
            if (selected.size() >= topN) break;

            Set<RuleCategory> candidateCategories = candidate.getRuleTriggers().stream()
                    .map(RuleTrigger::getCategory)
                    .collect(Collectors.toSet());

            boolean shouldAdd = true;
            if (selected.size() >= 3) {
                // 이미 선택된 카테고리와 겹치면 우선순위 낮춤
                if (usedCategories.containsAll(candidateCategories) && candidateCategories.size() > 0) {
                    shouldAdd = false;
                }
            }

            if (shouldAdd) {
                selected.add(candidate);
                usedCategories.addAll(candidateCategories);
            }
        }

        // 남은 자리는 점수 순으로 채움
        if (selected.size() < topN) {
            for (ClauseCandidate candidate : candidates) {
                if (selected.contains(candidate)) continue;
                if (selected.size() >= topN) break;
                selected.add(candidate);
            }
        }

        // 후보가 없으면 길이/대표성 기반으로 top 5
        if (selected.isEmpty() && !candidates.isEmpty()) {
            candidates.sort((a, b) -> Integer.compare(b.getText().length(), a.getText().length()));
            selected = candidates.stream().limit(5).collect(Collectors.toList());
        }

        return selected;
    }

    private void compilePatterns() {
        compiledPatterns = new HashMap<>();
        for (RulePattern rule : catalogLoader.getRules()) {
            List<CompiledPattern> compiled = new ArrayList<>();
            if (rule.getRegex() != null) {
                for (String regex : rule.getRegex()) {
                    try {
                        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                        compiled.add(new CompiledPattern(pattern, regex));
                    } catch (Exception e) {
                        log.warn("Failed to compile regex for rule {}: {}", rule.getId(), regex, e);
                    }
                }
            }
            compiledPatterns.put(rule.getId(), compiled);
        }
        log.info("Compiled {} rule patterns", compiledPatterns.size());
    }

    private static class CompiledPattern {
        final Pattern pattern;
        final String originalRegex;

        CompiledPattern(Pattern pattern, String originalRegex) {
            this.pattern = pattern;
            this.originalRegex = originalRegex;
        }
    }
}

