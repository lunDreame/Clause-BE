package com.clause.app.domain.llm;

import com.clause.app.domain.rules.engine.RuleCatalogLoader;
import com.clause.app.domain.rules.enums.ContractType;
import com.clause.app.domain.rules.enums.UserProfile;
import com.clause.app.domain.rules.model.ClauseCandidate;
import com.clause.app.domain.rules.model.RulePattern;
import com.clause.app.domain.rules.model.RuleTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final RuleCatalogLoader ruleCatalogLoader;

    private static final String SYSTEM_PROMPT = """
            CRITICAL: OUTPUT ONLY VALID JSON. NO MARKDOWN CODE BLOCKS. NO EXPLANATIONS. NO EXTRA TEXT BEFORE OR AFTER JSON.
            If you output anything other than pure JSON, the entire response will be considered invalid.
            
            You are a contract analysis assistant. You are NOT a lawyer. This is NOT legal advice.
            
            IMPORTANT LANGUAGE GUIDELINES FOR KOREAN OUTPUT:
            - Never use absolute or definitive Korean words: 불법 (illegal), 위법 (violation), 무효 (invalid), 반드시 (must), 확실히 (certainly), 100%, 절대 (absolutely), 무조건 (unconditionally)
            - Always use cautious, conditional phrasing in Korean (e.g., "~할 수 있어요", "~일 수 있어요", "~가능성이 있어요")
            - Use polite, non-confrontational language
            - Avoid making definitive legal judgments
            
            JSON REQUIREMENTS:
            - Return JSON that EXACTLY matches the provided schema
            - All required fields must be present
            - No extra keys beyond the schema
            - No missing required keys
            - All string values must be properly escaped
            - Arrays must not be empty when required
            - Numbers must be valid integers (no decimals for counts)
            """;

    private static final String DISCLAIMER = "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다.";

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildDeveloperPrompt(ContractType contractType, UserProfile userProfile, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert contract clause analyzer. Your task is to analyze the provided contract clauses and return a comprehensive analysis in JSON format.\n\n");
        
        sb.append("## Analysis Context\n");
        sb.append("The following context information is provided to help you understand the contract type and user profile:\n");
        sb.append("- Contract Type: ").append(contractType.name()).append("\n");
        sb.append("  This indicates the type of contract being analyzed (e.g., FREELANCER, EMPLOYMENT, LEASE, etc.)\n");
        sb.append("- User Profile: ").append(userProfile.name()).append("\n");
        sb.append("  This represents the user's background (e.g., STUDENT, ENTRY_LEVEL, FREELANCER, etc.)\n");
        sb.append("- Language: ").append(language).append("\n");
        sb.append("  The output language for the analysis (currently Korean: ko-KR)\n\n");

        sb.append("## Analysis Tasks\n");
        sb.append("You must perform the following analysis tasks for each contract clause:\n\n");
        sb.append("1. **Risk Assessment and Labeling**: Evaluate the risk level of each clause and assign an appropriate label (WARNING, CHECK, or OK)\n");
        sb.append("   - Consider the rule triggers detected for each clause\n");
        sb.append("   - Consider the severity and weight of detected rules\n");
        sb.append("   - Consider the contract type and user profile context\n\n");
        sb.append("2. **Risk Reason**: Explain why the clause might be concerning (1-2 sentences in Korean)\n");
        sb.append("   - Use cautious, non-definitive language\n");
        sb.append("   - Avoid absolute statements\n");
        sb.append("   - Focus on potential issues rather than definitive problems\n\n");
        sb.append("3. **Items to Confirm**: List specific items that need verification (1-4 items)\n");
        sb.append("   - Focus on concrete details: numbers, dates, conditions, scope\n");
        sb.append("   - Be specific about what information is missing or unclear\n\n");
        sb.append("4. **Soft Suggestions**: Provide gentle, diplomatic suggestions (1-3 items)\n");
        sb.append("   - Use question format when possible (e.g., \"~을 고려해볼 수 있을까요?\")\n");
        sb.append("   - Use polite, non-confrontational language\n");
        sb.append("   - Frame as recommendations rather than demands\n\n");
        sb.append("5. **Negotiation Suggestions**: Provide overall negotiation suggestions for the entire contract\n");
        sb.append("   - Consider the contract as a whole\n");
        sb.append("   - Prioritize the most important issues\n");
        sb.append("   - Provide actionable, realistic suggestions\n\n");
        sb.append("6. **Key Points Summary**: Extract and summarize the most critical points from the contract\n");
        sb.append("   - Focus on high-risk clauses\n");
        sb.append("   - Highlight unusual or non-standard terms\n");
        sb.append("   - Provide a concise overview (3-7 key points)\n\n");

        sb.append("## Label Definitions\n");
        sb.append("You must assign one of the following labels to each clause based on its risk level:\n\n");
        sb.append("- **WARNING**: Clauses that are commonly disputed or potentially unfavorable to the individual\n");
        sb.append("  - Examples: unlimited liability, unilateral termination rights, excessive penalties\n");
        sb.append("  - These clauses typically have high severity (WARNING) and high weight in the rule catalog\n");
        sb.append("  - Use this label when rule triggers indicate significant risk\n\n");
        sb.append("- **CHECK**: Clauses requiring context verification (numbers, periods, scope, conditions)\n");
        sb.append("  - Examples: payment terms, notice periods, scope of work, termination conditions\n");
        sb.append("  - These may be acceptable depending on specific values or context\n");
        sb.append("  - Use this label when the clause structure is standard but details need verification\n\n");
        sb.append("- **OK**: Relatively standard clauses with low risk\n");
        sb.append("  - Examples: standard definitions, general provisions, standard legal language\n");
        sb.append("  - These clauses are typically acceptable and don't raise concerns\n");
        sb.append("  - Use this label sparingly - most clauses should be WARNING or CHECK\n\n");

        sb.append("## Rule Catalog Information\n");
        sb.append("The following rule catalog is used for contract analysis. When rule triggers are detected, refer to the corresponding rule information to guide your analysis.\n\n");
        sb.append("**How to use the rule catalog:**\n");
        sb.append("- Each rule has an ID (e.g., R-W-PEN-001), category, severity, and weight\n");
        sb.append("- Rules with higher weights indicate higher risk\n");
        sb.append("- Severity indicates the type of concern: WARNING (serious) or CHECK (needs verification)\n");
        sb.append("- Boost values show how important a rule is for specific contract types\n");
        sb.append("- When a rule trigger is detected, consider its severity and weight when assigning labels\n");
        sb.append("- Rules with WARNING severity and high weight should typically result in WARNING labels\n");
        sb.append("- Rules with CHECK severity may result in CHECK labels, depending on context\n\n");
        
        List<RulePattern> allRules = ruleCatalogLoader.getRules();
        Map<String, RulePattern> rulesById = allRules.stream()
                .collect(Collectors.toMap(RulePattern::getId, r -> r));
        
        sb.append("### Complete Rule List (Weights shown for Contract Type: ").append(contractType.name()).append(")\n\n");
        sb.append("**Total Rules:** ").append(allRules.size()).append("\n\n");
        
        for (RulePattern rule : allRules) {
            int boost = rule.getBoost() != null ? rule.getBoost().getOrDefault(contractType.name(), 0) : 0;
            int totalWeight = rule.getBaseWeight() + boost;
            
            sb.append("- **Rule ID: ").append(rule.getId()).append("** | Severity: ").append(rule.getSeverity().name()).append("\n");
            sb.append("  - **Category:** ").append(rule.getCategory().name()).append("\n");
            sb.append("  - **Description:** ").append(rule.getDescription()).append("\n");
            sb.append("  - **Base Weight:** ").append(rule.getBaseWeight());
            if (boost > 0) {
                sb.append(" | **Boost for ").append(contractType.name()).append(":** +").append(boost);
                sb.append(" | **Total Weight:** ").append(totalWeight);
            }
            sb.append("\n");
            sb.append("  - **Interpretation:** ");
            if (rule.getSeverity().name().equals("WARNING")) {
                sb.append("This rule indicates a potentially serious issue. Clauses triggering this rule should typically be labeled as WARNING, especially if the total weight is high (>=3).");
            } else {
                sb.append("This rule indicates a clause that needs verification. Clauses triggering this rule may be labeled as CHECK, depending on the specific context and values.");
            }
            sb.append("\n\n");
        }
        sb.append("\n");

        sb.append("## Required JSON Schema (ALL fields are MANDATORY)\n");
        sb.append("You MUST return a JSON object with the following exact structure:\n\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"overall_summary\": {\n");
        sb.append("    \"warning_count\": <integer>,\n");
        sb.append("    \"check_count\": <integer>,\n");
        sb.append("    \"ok_count\": <integer>,\n");
        sb.append("    \"key_points\": [<string>, <string>, ...]\n");
        sb.append("  },\n");
        sb.append("  \"items\": [\n");
        sb.append("    {\n");
        sb.append("      \"clause_id\": <string>,\n");
        sb.append("      \"title\": <string>,\n");
        sb.append("      \"label\": \"WARNING\" | \"CHECK\" | \"OK\",\n");
        sb.append("      \"risk_reason\": <string>,\n");
        sb.append("      \"what_to_confirm\": [<string>, <string>, ...],\n");
        sb.append("      \"soft_suggestion\": [<string>, <string>, ...],\n");
        sb.append("      \"triggers\": [<string>, <string>, ...]\n");
        sb.append("    },\n");
        sb.append("    ...\n");
        sb.append("  ],\n");
        sb.append("  \"negotiation_suggestions\": [<string>, <string>, ...],\n");
        sb.append("  \"disclaimer\": \"").append(DISCLAIMER).append("\"\n");
        sb.append("}\n");
        sb.append("```\n\n");

        sb.append("## Detailed Field Requirements\n\n");
        sb.append("### overall_summary (REQUIRED object)\n");
        sb.append("- **warning_count** (integer, REQUIRED): Count of items labeled as WARNING\n");
        sb.append("  - Must match the actual number of WARNING items in the items array\n");
        sb.append("  - Must be >= 0\n\n");
        sb.append("- **check_count** (integer, REQUIRED): Count of items labeled as CHECK\n");
        sb.append("  - Must match the actual number of CHECK items in the items array\n");
        sb.append("  - Must be >= 0\n\n");
        sb.append("- **ok_count** (integer, REQUIRED): Count of items labeled as OK\n");
        sb.append("  - Must match the actual number of OK items in the items array\n");
        sb.append("  - Must be >= 0\n\n");
        sb.append("- **key_points** (array of strings, REQUIRED): Summary of critical points\n");
        sb.append("  - Must contain 3-7 key points\n");
        sb.append("  - Each point should be a concise sentence in Korean\n");
        sb.append("  - Focus on the most important risks and concerns\n");
        sb.append("  - Array must not be empty\n\n");
        
        sb.append("### items (REQUIRED array)\n");
        sb.append("- **MUST NOT be empty** - at least one item is required\n");
        sb.append("- Each item represents one contract clause analysis\n");
        sb.append("- **clause_id** (string, REQUIRED): The exact ID from the input clause\n");
        sb.append("  - Must match the ID provided in the input exactly\n");
        sb.append("  - Do not modify or generate new IDs\n\n");
        sb.append("- **title** (string, REQUIRED): The title/heading of the clause\n");
        sb.append("  - Should match or be derived from the input clause title\n");
        sb.append("  - Use Korean if the contract is in Korean\n\n");
        sb.append("- **label** (string, REQUIRED): One of \"WARNING\", \"CHECK\", or \"OK\"\n");
        sb.append("  - Must be exactly one of these three values (case-sensitive)\n");
        sb.append("  - Consider rule triggers, severity, and weight when assigning\n");
        sb.append("  - Most clauses should be WARNING or CHECK, not OK\n\n");
        sb.append("- **risk_reason** (string, REQUIRED): Explanation of why the clause is concerning\n");
        sb.append("  - Must be 1-2 sentences in Korean\n");
        sb.append("  - Use cautious, non-definitive language\n");
        sb.append("  - Examples of good phrasing: \"~할 수 있어요\", \"~일 수 있어요\", \"~가능성이 있어요\"\n");
        sb.append("  - Avoid: \"불법\", \"위법\", \"무효\", \"반드시\", \"확실히\", \"100%\", \"절대\", \"무조건\"\n");
        sb.append("  - Must not be empty\n\n");
        sb.append("- **what_to_confirm** (array of strings, REQUIRED): Items that need verification\n");
        sb.append("  - Must contain 1-4 items\n");
        sb.append("  - Each item should be specific and actionable\n");
        sb.append("  - Focus on concrete details: numbers, dates, conditions, scope\n");
        sb.append("  - Array must not be empty\n\n");
        sb.append("- **soft_suggestion** (array of strings, REQUIRED): Gentle, diplomatic suggestions\n");
        sb.append("  - Must contain 1-3 items\n");
        sb.append("  - Use question format when possible (e.g., \"~을 고려해볼 수 있을까요?\")\n");
        sb.append("  - Use polite, non-confrontational language\n");
        sb.append("  - Array must not be empty\n\n");
        sb.append("- **triggers** (array of strings, REQUIRED): List of rule IDs that were triggered\n");
        sb.append("  - Must contain the exact rule IDs (e.g., \"R-W-PEN-001\") from the detected triggers\n");
        sb.append("  - Use the rule IDs provided in the input, not category names\n");
        sb.append("  - If no triggers were detected, use an empty array []\n");
        sb.append("  - Do not modify or generate rule IDs\n\n");
        
        sb.append("### negotiation_suggestions (REQUIRED array of strings)\n");
        sb.append("- Must contain at least 1 suggestion\n");
        sb.append("- Each suggestion should be actionable and realistic\n");
        sb.append("  - Focus on the most important issues from the contract\n");
        sb.append("  - Prioritize high-risk clauses\n");
        sb.append("  - Provide specific, concrete suggestions\n");
        sb.append("  - Use polite, diplomatic language in Korean\n");
        sb.append("- Array must not be empty\n\n");
        
        sb.append("### disclaimer (REQUIRED string)\n");
        sb.append("- Must be EXACTLY: \"").append(DISCLAIMER).append("\"\n");
        sb.append("- Do not modify this text\n");
        sb.append("- Do not translate or paraphrase\n\n");
        
        sb.append("## Critical Validation Rules\n");
        sb.append("Before returning your response, verify:\n");
        sb.append("1. The JSON is valid and parseable\n");
        sb.append("2. All required fields are present\n");
        sb.append("3. No extra fields beyond the schema\n");
        sb.append("4. warning_count + check_count + ok_count = items.length\n");
        sb.append("5. All arrays are non-empty (except triggers which can be empty if no triggers detected)\n");
        sb.append("6. All clause_id values match the input IDs exactly\n");
        sb.append("7. All triggers arrays contain valid rule IDs from the input\n");
        sb.append("8. The disclaimer field matches exactly\n");
        sb.append("9. All Korean text uses cautious, non-definitive language\n");

        return sb.toString();
    }

    public String buildUserPrompt(List<ClauseCandidate> candidates, ContractType contractType, UserProfile userProfile, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Contract Clauses to Analyze\n\n");
        sb.append("Below are the contract clauses that need to be analyzed. Each clause includes its ID, title, content, and any detected rule triggers.\n\n");
        
        Map<String, RulePattern> rulesById = ruleCatalogLoader.getRules().stream()
                .collect(Collectors.toMap(RulePattern::getId, r -> r));
        
        Set<String> detectedRuleIds = new HashSet<>();
        
        sb.append("**Total clauses to analyze:** ").append(candidates.size()).append("\n\n");
        
        for (int i = 0; i < candidates.size(); i++) {
            ClauseCandidate candidate = candidates.get(i);
            sb.append("### Clause ").append(i + 1).append("\n");
            sb.append("- **Clause ID:** ").append(candidate.getId()).append("\n");
            sb.append("  - This ID must be used exactly as-is in the clause_id field of your response\n\n");
            sb.append("- **Title:** ").append(candidate.getTitle()).append("\n");
            sb.append("  - Use this or derive a similar title for the title field\n\n");
            sb.append("- **Clause Content:**\n");
            sb.append("```\n");
            sb.append(candidate.getText()).append("\n");
            sb.append("```\n\n");
            
            if (!candidate.getRuleTriggers().isEmpty()) {
                sb.append("- **Detected Rule Triggers:**\n");
                sb.append("  The following rules were triggered by patterns found in this clause:\n\n");
                for (RuleTrigger trigger : candidate.getRuleTriggers()) {
                    detectedRuleIds.add(trigger.getRuleId());
                    RulePattern rule = rulesById.get(trigger.getRuleId());
                    if (rule != null) {
                        sb.append("  - **Rule ID:** ").append(trigger.getRuleId()).append("\n");
                        sb.append("    - **Category:** ").append(trigger.getCategory().name()).append("\n");
                        sb.append("    - **Severity:** ").append(trigger.getSeverity().name());
                        if (trigger.getSeverity().name().equals("WARNING")) {
                            sb.append(" (This indicates a potentially serious issue)");
                        } else {
                            sb.append(" (This indicates a clause that needs verification)");
                        }
                        sb.append("\n");
                        sb.append("    - **Weight:** ").append(trigger.getWeight());
                        if (trigger.getWeight() >= 4) {
                            sb.append(" (HIGH - This is a high-priority concern)");
                        } else if (trigger.getWeight() >= 2) {
                            sb.append(" (MEDIUM - This is a moderate concern)");
                        } else {
                            sb.append(" (LOW - This is a minor concern)");
                        }
                        sb.append("\n");
                        sb.append("    - **Description:** ").append(rule.getDescription()).append("\n");
                        sb.append("    - **Matched Text:** \"").append(trigger.getMatchedText()).append("\"\n");
                        sb.append("      - This is the specific text in the clause that triggered this rule\n\n");
                    } else {
                        sb.append("  - **Rule ID:** ").append(trigger.getRuleId()).append("\n");
                        sb.append("    - **Category:** ").append(trigger.getCategory().name()).append("\n\n");
                    }
                }
                sb.append("  **Analysis Guidance:**\n");
                sb.append("  - Consider the severity and weight of these triggers when assigning the label\n");
                sb.append("  - Multiple WARNING triggers with high weights should result in a WARNING label\n");
                sb.append("  - CHECK triggers may result in a CHECK label, depending on context\n");
                sb.append("  - Include all triggered rule IDs in the triggers array of your response\n\n");
            } else {
                sb.append("- **Detected Rule Triggers:** None\n");
                sb.append("  - No rule patterns were detected in this clause\n");
                sb.append("  - The triggers array should be empty [] for this clause\n\n");
            }
        }
        
        if (!detectedRuleIds.isEmpty()) {
            sb.append("## Summary of Detected Rules\n");
            sb.append("The following rules were triggered across all clauses. Use this information to guide your analysis:\n\n");
            for (String ruleId : detectedRuleIds) {
                RulePattern rule = rulesById.get(ruleId);
                if (rule != null) {
                    sb.append("- **").append(ruleId).append("**: ").append(rule.getDescription());
                    sb.append(" (Severity: ").append(rule.getSeverity().name()).append(")\n");
                }
            }
            sb.append("\n");
            sb.append("**Important:** When assigning labels to clauses:\n");
            sb.append("- Clauses with WARNING severity rules and high weights (>=3) should typically be labeled WARNING\n");
            sb.append("- Clauses with CHECK severity rules may be labeled CHECK, depending on the specific context\n");
            sb.append("- Consider the cumulative effect of multiple triggers on the same clause\n");
            sb.append("- Higher weight rules indicate higher priority concerns\n\n");
        }
        
        sb.append("## Analysis Instructions\n\n");
        sb.append("Analyze all the clauses above and return your analysis in JSON format following the schema provided in the developer prompt.\n\n");
        
        sb.append("## Critical: triggers Field Values\n");
        sb.append("For each clause, the **triggers** field must contain the exact list of rule IDs that were detected for that clause.\n\n");
        sb.append("**Format:** An array of rule ID strings\n");
        sb.append("**Example:** [\"R-W-PEN-001\", \"R-W-DMG-001\"]\n\n");
        sb.append("**Mapping of clauses to rule IDs:**\n");
        for (int i = 0; i < candidates.size(); i++) {
            ClauseCandidate candidate = candidates.get(i);
            if (!candidate.getRuleTriggers().isEmpty()) {
                List<String> ruleIds = candidate.getRuleTriggers().stream()
                        .map(RuleTrigger::getRuleId)
                        .distinct()
                        .collect(Collectors.toList());
                sb.append("- **Clause ").append(i + 1).append("** (ID: ").append(candidate.getId()).append("): ");
                sb.append("triggers = ").append(ruleIds).append("\n");
            } else {
                sb.append("- **Clause ").append(i + 1).append("** (ID: ").append(candidate.getId()).append("): ");
                sb.append("triggers = [] (no triggers detected)\n");
            }
        }
        sb.append("\n");
        
        sb.append("## Final Checklist\n");
        sb.append("Before returning your response, ensure:\n");
        sb.append("1. ✅ All clause IDs from the input are included in the items array\n");
        sb.append("2. ✅ Each clause_id matches the input ID exactly\n");
        sb.append("3. ✅ Each triggers array contains the correct rule IDs for that clause\n");
        sb.append("4. ✅ warning_count + check_count + ok_count = total number of items\n");
        sb.append("5. ✅ All required fields are present and non-empty (except triggers which can be empty)\n");
        sb.append("6. ✅ The disclaimer field matches exactly\n");
        sb.append("7. ✅ All Korean text uses cautious, non-definitive language\n");
        sb.append("8. ✅ The JSON is valid and parseable\n\n");
        
        sb.append("Now analyze the clauses and return your response as valid JSON.");

        return sb.toString();
    }
}

