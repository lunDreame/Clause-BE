package com.clause.app.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JsonRepairUtilTest {

    @Autowired
    private JsonRepairUtil jsonRepairUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testExtractJsonFromMarkdown() throws Exception {
        String rawText = "```json\n{\"test\": \"value\"}\n```";
        String repaired = jsonRepairUtil.extractAndRepair(rawText);
        assertThat(repaired).contains("\"test\"");
        assertThat(jsonRepairUtil.isValidJson(repaired)).isTrue();
    }

    @Test
    void testExtractJsonWithExtraText() throws Exception {
        String rawText = "Here is the JSON:\n{\"test\": \"value\"}\nThat's it.";
        String repaired = jsonRepairUtil.extractAndRepair(rawText);
        assertThat(jsonRepairUtil.isValidJson(repaired)).isTrue();
    }

    @Test
    void testSmartQuotesReplacement() throws Exception {
        String rawText = "{'test': 'value'}";
        // Smart quotes는 이미 일반 quotes로 변환되어야 함
        String repaired = jsonRepairUtil.extractAndRepair(rawText.replace("'", "\""));
        assertThat(jsonRepairUtil.isValidJson(repaired)).isTrue();
    }
}

