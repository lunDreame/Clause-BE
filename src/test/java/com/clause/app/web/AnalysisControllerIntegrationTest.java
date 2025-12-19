package com.clause.app.web;

import com.clause.app.domain.analysis.dto.AnalysisRequest;
import com.clause.app.domain.document.entity.Document;
import com.clause.app.domain.document.repo.DocumentRepository;
import com.clause.app.domain.llm.LlmClient;
import com.clause.app.domain.llm.dto.LlmRequest;
import com.clause.app.domain.llm.dto.LlmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @MockBean
    private LlmClient llmClient;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID documentId;

    @BeforeEach
    void setUp() {
        Document document = Document.builder()
                .originalFileName("test.pdf")
                .contentType("application/pdf")
                .sizeBytes(1000L)
                .storagePath("test.pdf")
                .extractedText("제1조 테스트\n제2조 손해배상 무제한")
                .textSha256("test-hash")
                .build();
        document = documentRepository.save(document);
        documentId = document.getId();
    }

    @Test
    void testAnalysisWithValidJson() throws Exception {
        String validJson = """
                {
                  "overall_summary": {
                    "warning_count": 1,
                    "check_count": 0,
                    "ok_count": 0,
                    "key_points": ["테스트"]
                  },
                  "items": [{
                    "clause_id": "C-001",
                    "title": "제1조",
                    "label": "WARNING",
                    "risk_reason": "주의가 필요해요.",
                    "what_to_confirm": ["확인사항"],
                    "soft_suggestion": ["제안사항"],
                    "triggers": ["TEST"]
                  }],
                  "negotiation_suggestions": ["협상 제안"],
                  "disclaimer": "면책"
                }
                """;

        when(llmClient.call(any(LlmRequest.class)))
                .thenReturn(LlmResponse.builder()
                        .content(validJson)
                        .model("gpt-4o-mini")
                        .build());

        AnalysisRequest request = AnalysisRequest.builder()
                .documentId(documentId)
                .contractType("FREELANCER")
                .userProfile("FREELANCER")
                .language("ko-KR")
                .build();

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Request-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallSummary").exists());
    }

    @Test
    void testAnalysisWithMarkdownJson() throws Exception {
        String markdownJson = "```json\n" + """
                {
                  "overall_summary": {
                    "warning_count": 0,
                    "check_count": 1,
                    "ok_count": 0,
                    "key_points": ["테스트"]
                  },
                  "items": [{
                    "clause_id": "C-001",
                    "title": "제1조",
                    "label": "CHECK",
                    "risk_reason": "확인이 필요해요.",
                    "what_to_confirm": ["확인사항"],
                    "soft_suggestion": ["제안사항"],
                    "triggers": ["TEST"]
                  }],
                  "negotiation_suggestions": ["협상 제안"],
                  "disclaimer": "면책"
                }
                """ + "\n```";

        when(llmClient.call(any(LlmRequest.class)))
                .thenReturn(LlmResponse.builder()
                        .content(markdownJson)
                        .model("gpt-4o-mini")
                        .build());

        AnalysisRequest request = AnalysisRequest.builder()
                .documentId(documentId)
                .contractType("FREELANCER")
                .userProfile("FREELANCER")
                .language("ko-KR")
                .build();

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Request-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

