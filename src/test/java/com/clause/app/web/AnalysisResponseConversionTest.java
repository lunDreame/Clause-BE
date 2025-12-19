package com.clause.app.web;

import com.clause.app.domain.analysis.dto.AnalysisResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AnalysisResponseConversionTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSnakeCaseToCamelCaseConversion() throws Exception {
        String snakeCaseJson = """
                {
                  "clause_id": "C-001",
                  "title": "제1조",
                  "label": "WARNING",
                  "risk_reason": "주의가 필요해요.",
                  "what_to_confirm": ["확인사항1", "확인사항2"],
                  "soft_suggestion": ["제안사항1"],
                  "triggers": ["R-W-PEN-001"]
                }
                """;

        JsonNode jsonNode = objectMapper.readTree(snakeCaseJson);
        
        AnalysisResponse.AnalysisItem item = objectMapper.convertValue(
                jsonNode,
                AnalysisResponse.AnalysisItem.class
        );

        assertNotNull(item);
        assertEquals("C-001", item.getClauseId());
        assertEquals("제1조", item.getTitle());
        assertEquals("WARNING", item.getLabel());
        assertEquals("주의가 필요해요.", item.getRiskReason());
        assertNotNull(item.getWhatToConfirm());
        assertEquals(2, item.getWhatToConfirm().size());
        assertNotNull(item.getSoftSuggestion());
        assertEquals(1, item.getSoftSuggestion().size());
        assertNotNull(item.getTriggers());
        assertEquals(1, item.getTriggers().size());
        assertEquals("R-W-PEN-001", item.getTriggers().get(0));
    }

    @Test
    void testItemsArrayConversion() throws Exception {
        String itemsArrayJson = """
                [
                  {
                    "clause_id": "C-001",
                    "title": "제1조",
                    "label": "WARNING",
                    "risk_reason": "주의가 필요해요.",
                    "what_to_confirm": ["확인사항"],
                    "soft_suggestion": ["제안사항"],
                    "triggers": ["R-W-PEN-001"]
                  },
                  {
                    "clause_id": "C-002",
                    "title": "제2조",
                    "label": "CHECK",
                    "risk_reason": "확인이 필요해요.",
                    "what_to_confirm": ["확인사항2"],
                    "soft_suggestion": ["제안사항2"],
                    "triggers": []
                  }
                ]
                """;

        JsonNode itemsNode = objectMapper.readTree(itemsArrayJson);
        
        List<AnalysisResponse.AnalysisItem> items = objectMapper.convertValue(
                itemsNode,
                new TypeReference<List<AnalysisResponse.AnalysisItem>>() {}
        );

        assertNotNull(items);
        assertEquals(2, items.size());
        
        AnalysisResponse.AnalysisItem item1 = items.get(0);
        assertEquals("C-001", item1.getClauseId());
        assertEquals("WARNING", item1.getLabel());
        
        AnalysisResponse.AnalysisItem item2 = items.get(1);
        assertEquals("C-002", item2.getClauseId());
        assertEquals("CHECK", item2.getLabel());
    }
}
