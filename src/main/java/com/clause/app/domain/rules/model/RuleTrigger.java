package com.clause.app.domain.rules.model;

import com.clause.app.domain.rules.enums.RuleCategory;
import com.clause.app.domain.rules.enums.RuleSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTrigger {
    private String ruleId;
    private RuleCategory category;
    private RuleSeverity severity;
    private int weight;
    private String matchedText;
    private int startIndex;
    private int endIndex;
}

