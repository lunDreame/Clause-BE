package com.clause.app.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없어요."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없어요."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식이에요."),
    OCR_NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "이미지 OCR은 아직 지원하지 않아요. PDF로 업로드해 주세요."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 용량이 너무 커요(최대 10MB)."),
    EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "텍스트 추출에 실패했어요."),
    LLM_UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "분석 엔진 응답이 불안정해요. 잠시 후 다시 시도해 주세요."),
    JSON_REPAIR_FAILED(HttpStatus.BAD_GATEWAY, "분석 결과 형식이 올바르지 않아 처리하지 못했어요."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많아요. 잠시 후 다시 시도해 주세요."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않아요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했어요.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}

