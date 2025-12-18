-- 인덱스 생성
CREATE INDEX idx_analysis_result_document_id_created_at ON analysis_result(document_id, created_at DESC);
CREATE INDEX idx_document_text_sha256 ON document(text_sha256);
CREATE INDEX idx_analysis_result_status ON analysis_result(status);

