package com.clause.app.domain.llm;

import com.clause.app.common.ClauseException;
import com.clause.app.common.ErrorCode;
import com.clause.app.domain.llm.dto.LlmRequest;
import com.clause.app.domain.llm.dto.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HttpLlmClient implements LlmClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultModel;
    private final int timeoutMs;

    public HttpLlmClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${clause.llm.base-url}") String baseUrl,
            @Value("${clause.llm.api-key:}") String apiKey,
            @Value("${clause.llm.model:gpt-4o-mini}") String defaultModel,
            @Value("${clause.llm.timeout-ms:10000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.timeoutMs = timeoutMs;

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    @Retry(name = "llmRetry")
    @CircuitBreaker(name = "llmCircuitBreaker", fallbackMethod = "fallback")
    public LlmResponse call(LlmRequest request) {
        try {
            Map<String, Object> payload = buildPayload(request);

            return webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .retryWhen(RetryBackoffSpec.fixedDelay(2, Duration.ofMillis(500)))
                    .map(this::parseResponse)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("LLM API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ClauseException(ErrorCode.LLM_UPSTREAM_ERROR, "LLM API 호출 실패: " + e.getMessage());
        } catch (Exception e) {
            log.error("LLM call failed", e);
            throw new ClauseException(ErrorCode.LLM_UPSTREAM_ERROR, e);
        }
    }

    private Map<String, Object> buildPayload(LlmRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        payload.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.3);
        payload.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 4000);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", request.getSystemPrompt());
        messages.add(systemMsg);
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", request.getDeveloperPrompt() + "\n\n" + request.getUserPrompt());
        messages.add(userMsg);
        payload.put("messages", messages);

        return payload;
    }

    private LlmResponse parseResponse(JsonNode json) {
        try {
            JsonNode choices = json.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new IllegalArgumentException("No choices in response");
            }

            String content = choices.get(0).get("message").get("content").asText();
            String model = json.has("model") ? json.get("model").asText() : defaultModel;

            JsonNode usage = json.get("usage");
            Integer promptTokens = usage != null && usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
            Integer completionTokens = usage != null && usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            Integer totalTokens = usage != null && usage.has("total_tokens") ? usage.get("total_tokens").asInt() : null;

            return LlmResponse.builder()
                    .content(content)
                    .model(model)
                    .usageTokens(totalTokens)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse LLM response", e);
            throw new ClauseException(ErrorCode.LLM_UPSTREAM_ERROR, "LLM 응답 파싱 실패: " + e.getMessage());
        }
    }

    public LlmResponse fallback(LlmRequest request, Exception e) {
        log.error("LLM fallback triggered", e);
        throw new ClauseException(ErrorCode.LLM_UPSTREAM_ERROR, "LLM 서비스가 일시적으로 사용할 수 없습니다.");
    }
}

