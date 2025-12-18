package com.clause.app.domain.rules.model;

import com.clause.app.domain.rules.enums.RuleCategory;
import com.clause.app.domain.rules.enums.RuleSeverity;
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
public class RulePattern {
    private String id;
    private RuleCategory category;
    private RuleSeverity severity;
    private int baseWeight;
    private Map<String, Integer> boost; // ContractType -> boost value
    private List<String> regex;
    private String description;
}

