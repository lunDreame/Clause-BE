package com.clause.app.domain.rules.engine;

import com.clause.app.domain.rules.enums.RuleCategory;
import com.clause.app.domain.rules.enums.RuleSeverity;
import com.clause.app.domain.rules.model.RulePattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RuleCatalogLoader {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final String rulesPath;
    private List<RulePattern> rules;

    public RuleCatalogLoader(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            @Value("${clause.rules.path}") String rulesPath) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.rulesPath = rulesPath;
        loadRules();
    }

    public List<RulePattern> getRules() {
        return rules;
    }

    public int getTotalRegexCount() {
        return rules.stream()
                .mapToInt(r -> r.getRegex() != null ? r.getRegex().size() : 0)
                .sum();
    }

    private void loadRules() {
        try {
            Resource resource = resourceLoader.getResource(rulesPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Rules file not found: " + rulesPath);
            }

            Yaml yaml = new Yaml();
            try (InputStream inputStream = resource.getInputStream()) {
                Map<String, Object> data = yaml.load(inputStream);
                List<Map<String, Object>> rulesList = (List<Map<String, Object>>) data.get("rules");

                rules = rulesList.stream()
                        .map(this::mapToRulePattern)
                        .collect(Collectors.toList());

                log.info("Loaded {} rules with {} total regex patterns", rules.size(), getTotalRegexCount());
            }
        } catch (Exception e) {
            log.error("Failed to load rules from {}", rulesPath, e);
            throw new IllegalStateException("Failed to load rules", e);
        }
    }

    private RulePattern mapToRulePattern(Map<String, Object> map) {
        RulePattern.RulePatternBuilder builder = RulePattern.builder()
                .id((String) map.get("id"))
                .category(RuleCategory.valueOf((String) map.get("category")))
                .severity(RuleSeverity.valueOf((String) map.get("severity")))
                .baseWeight(((Number) map.get("baseWeight")).intValue())
                .description((String) map.get("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> boostMap = (Map<String, Object>) map.get("boost");
        if (boostMap != null) {
            Map<String, Integer> boost = new HashMap<>();
            boostMap.forEach((k, v) -> boost.put(k, ((Number) v).intValue()));
            builder.boost(boost);
        }

        @SuppressWarnings("unchecked")
        List<String> regexList = (List<String>) map.get("regex");
        builder.regex(regexList != null ? regexList : Collections.emptyList());

        return builder.build();
    }
}

