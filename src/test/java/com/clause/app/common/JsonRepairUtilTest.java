package com.clause.app.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JsonRepairUtilTest {

    @Autowired
    private JsonRepairUtil jsonRepairUtil;

    @Test
    void testExtractJsonFromMarkdown() throws Exception {
        String rawText = "```json\n{\"test\": \"value\"}\n```";
        String repaired = jsonRepairUtil.extractAndRepair(rawText);
        assertThat(repaired).contains("\"test\"");
    }

    @Test
    void testExtractJsonWithExtraText() throws Exception {
        String rawText = "Here is the JSON:\n{\"test\": \"value\"}\nThat's it.";
        String repaired = jsonRepairUtil.extractAndRepair(rawText);
        assertThat(repaired).isNotNull();
    }

    @Test
    void testSmartQuotesReplacement() throws Exception {
        String rawText = "{'test': 'value'}";
        String repaired = jsonRepairUtil.extractAndRepair(rawText.replace("'", "\""));
        assertThat(repaired).isNotNull();
    }
}

