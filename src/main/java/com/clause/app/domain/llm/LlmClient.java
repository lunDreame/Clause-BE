package com.clause.app.domain.llm;

import com.clause.app.domain.llm.dto.LlmRequest;
import com.clause.app.domain.llm.dto.LlmResponse;

public interface LlmClient {
    LlmResponse call(LlmRequest request);
}

