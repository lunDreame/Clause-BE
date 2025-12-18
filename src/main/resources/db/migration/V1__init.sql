-- Document 테이블
CREATE TABLE document (
    id UUID PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    extracted_text TEXT,
    text_sha256 VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AnalysisResult 테이블
CREATE TABLE analysis_result (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    contract_type VARCHAR(40) NOT NULL,
    user_profile VARCHAR(40) NOT NULL,
    language VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    overall_summary_json TEXT,
    items_json TEXT,
    negotiation_suggestions_json TEXT,
    disclaimer VARCHAR(400),
    rule_triggers_json TEXT,
    llm_model VARCHAR(100),
    llm_raw_json TEXT,
    error_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

