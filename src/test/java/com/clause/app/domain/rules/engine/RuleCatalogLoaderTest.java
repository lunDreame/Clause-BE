package com.clause.app.domain.rules.engine;

import com.clause.app.domain.rules.model.RulePattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RuleCatalogLoaderTest {

    @Autowired
    private RuleCatalogLoader loader;

    @Test
    void testLoadRules() {
        assertThat(loader.getRules()).isNotEmpty();
    }

    @Test
    void testTotalRegexCount() {
        int totalRegexCount = loader.getTotalRegexCount();
        assertThat(totalRegexCount).isGreaterThanOrEqualTo(260);
        System.out.println("Total regex count: " + totalRegexCount);
    }

    @Test
    void testRulePatternStructure() {
        RulePattern rule = loader.getRules().get(0);
        assertThat(rule.getId()).isNotNull();
        assertThat(rule.getCategory()).isNotNull();
        assertThat(rule.getSeverity()).isNotNull();
        assertThat(rule.getBaseWeight()).isGreaterThan(0);
        assertThat(rule.getRegex()).isNotEmpty();
    }

    @Test
    void testBoostValues() {
        // FREELANCE에 대한 boost가 있는 룰 찾기
        boolean hasFreelanceBoost = loader.getRules().stream()
                .anyMatch(r -> r.getBoost() != null && r.getBoost().containsKey("FREELANCE"));
        assertThat(hasFreelanceBoost).isTrue();

        // LEASE에 대한 boost가 있는 룰 찾기
        boolean hasLeaseBoost = loader.getRules().stream()
                .anyMatch(r -> r.getBoost() != null && r.getBoost().containsKey("LEASE"));
        assertThat(hasLeaseBoost).isTrue();
    }
}

