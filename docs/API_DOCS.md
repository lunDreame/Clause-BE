# Clause API 문서

## 개요

Clause API는 계약서 문서 분석을 위한 RESTful API입니다. 문서 업로드, 텍스트 추출, 계약서 분석 기능을 제공합니다.

## 기본 정보

- Base URL: `/api/v1`
- Content-Type: `application/json` (일부 엔드포인트는 `multipart/form-data`)
- 인증: 현재 인증 미구현 (Rate Limiting은 IP 기반)

## 공통 응답 형식

모든 API 응답은 다음 형식을 따릅니다:

### 성공 응답

```json
{
  "success": true,
  "data": { ... }
}
```

### 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": { ... }
  }
}
```

## 에러 코드

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| NOT_FOUND | 404 | 요청한 리소스를 찾을 수 없음 |
| DOCUMENT_NOT_FOUND | 404 | 문서를 찾을 수 없음 |
| UNSUPPORTED_FILE_TYPE | 400 | 지원하지 않는 파일 형식 |
| OCR_NOT_IMPLEMENTED | 501 | 이미지 OCR 미지원 (PDF만 지원) |
| FILE_TOO_LARGE | 413 | 파일 용량 초과 (최대 10MB) |
| EXTRACTION_FAILED | 500 | 텍스트 추출 실패 |
| LLM_UPSTREAM_ERROR | 502 | 분석 엔진 응답 불안정 |
| JSON_REPAIR_FAILED | 502 | 분석 결과 형식 오류 |
| RATE_LIMITED | 429 | 요청 한도 초과 |
| VALIDATION_ERROR | 400 | 요청 값 검증 실패 |
| INTERNAL_ERROR | 500 | 서버 내부 오류 |

## Rate Limiting

- 기본 제한: 분당 30회 요청
- 식별자: 클라이언트 IP 주소 (X-Forwarded-For 헤더 우선)
- 초과 시: HTTP 429 응답

## 엔드포인트

### Health Check

#### GET /api/v1/health

서버 상태를 확인합니다.

**요청**

```
GET /api/v1/health
```

**응답**

```json
{
  "success": true,
  "data": {
    "status": "UP"
  }
}
```

---

### 문서 관리

#### POST /api/v1/documents

문서를 업로드합니다.

**요청**

- Content-Type: `multipart/form-data`
- 파라미터:
  - `file` (MultipartFile, 필수): 업로드할 파일
    - 최대 크기: 10MB
    - 지원 형식: PDF

**응답**

```json
{
  "success": true,
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "originalFileName": "contract.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 1024000,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

**에러 응답**

- `FILE_TOO_LARGE`: 파일 크기 초과
- `UNSUPPORTED_FILE_TYPE`: 지원하지 않는 파일 형식
- `RATE_LIMITED`: 요청 한도 초과

---

#### GET /api/v1/documents/{id}

문서 정보를 조회합니다.

**요청**

- 경로 파라미터:
  - `id` (UUID, 필수): 문서 ID
- 쿼리 파라미터:
  - `includeText` (boolean, 선택): 추출된 텍스트 포함 여부 (기본값: false)

**응답 (includeText=false)**

```json
{
  "success": true,
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "originalFileName": "contract.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 1024000,
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

**응답 (includeText=true)**

```json
{
  "success": true,
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "originalFileName": "contract.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 1024000,
    "extractedText": "계약서 내용...",
    "textLength": 5000,
    "textSha256": "abc123...",
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

**에러 응답**

- `DOCUMENT_NOT_FOUND`: 문서를 찾을 수 없음
- `VALIDATION_ERROR`: 잘못된 UUID 형식
- `RATE_LIMITED`: 요청 한도 초과

---

#### POST /api/v1/documents/{id}/extract

문서에서 텍스트를 추출합니다.

**요청**

- 경로 파라미터:
  - `id` (UUID, 필수): 문서 ID

**응답**

```json
{
  "success": true,
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "textLength": 5000,
    "textSha256": "abc123..."
  }
}
```

**에러 응답**

- `DOCUMENT_NOT_FOUND`: 문서를 찾을 수 없음
- `EXTRACTION_FAILED`: 텍스트 추출 실패
- `OCR_NOT_IMPLEMENTED`: 이미지 OCR 미지원
- `VALIDATION_ERROR`: 잘못된 UUID 형식
- `RATE_LIMITED`: 요청 한도 초과

---

### 분석

#### POST /api/v1/analyses

계약서를 분석합니다.

**요청**

```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "contractType": "FREELANCER",
  "userProfile": "STUDENT",
  "language": "ko-KR"
}
```

**요청 필드**

- `documentId` (UUID, 필수): 분석할 문서 ID
- `contractType` (String, 필수): 계약서 유형
  - 가능한 값: `FREELANCER`, `EMPLOYMENT`, `PART_TIME`, `LEASE`, `NDA`, `OTHER`
- `userProfile` (String, 필수): 사용자 프로필
  - 가능한 값: `STUDENT`, `ENTRY_LEVEL`, `FREELANCER`, `INDIVIDUAL_BUSINESS`, `GENERAL_CONSUMER`
- `language` (String, 필수): 언어 코드
  - 현재 지원: `ko-KR`

**응답**

```json
{
  "success": true,
  "data": {
    "analysisId": "660e8400-e29b-41d4-a716-446655440000",
    "overallSummary": {
      "warningCount": 3,
      "checkCount": 5,
      "okCount": 12,
      "keyPoints": [
        "계약 기간이 명확하지 않음",
        "해지 조건이 불리함"
      ]
    },
    "items": [
      {
        "clauseId": "clause-001",
        "title": "계약 기간",
        "label": "WARNING",
        "riskReason": "계약 기간이 명시되지 않았습니다.",
        "whatToConfirm": [
          "계약 시작일과 종료일 확인",
          "자동 갱신 여부 확인"
        ],
        "softSuggestion": [
          "계약 기간을 명확히 명시하도록 요청"
        ],
        "triggers": [
          "R-W-PEN-001",
          "R-C-TER-001"
        ]
      }
    ],
    "negotiationSuggestions": [
      "계약 기간을 명확히 명시하도록 요청하세요",
      "해지 조건을 더 유리하게 변경하도록 협상하세요"
    ],
    "disclaimer": "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다."
  }
}
```

**응답 필드**

- `analysisId` (UUID): 분석 결과 ID
- `overallSummary` (Object): 전체 요약
  - `warningCount` (Integer): 경고 항목 수
  - `checkCount` (Integer): 확인 필요 항목 수
  - `okCount` (Integer): 정상 항목 수
  - `keyPoints` (List<String>): 주요 포인트 목록
- `items` (List<AnalysisItem>): 분석 항목 목록
  - `clauseId` (String): 조항 ID
  - `title` (String): 조항 제목
  - `label` (String): 레이블 (`WARNING`, `CHECK`, `OK`)
  - `riskReason` (String): 위험 사유
  - `whatToConfirm` (List<String>): 확인 사항 목록
  - `softSuggestion` (List<String>): 제안 사항 목록
  - `triggers` (List<String>): 트리거된 규칙 ID 목록 (예: "R-W-PEN-001", "R-C-TER-001")
- `negotiationSuggestions` (List<String>): 협상 제안 목록
- `disclaimer` (String): 면책 조항 (고정값: "Clause는 법률 자문이 아니며, 정보 제공 목적입니다. 중요한 계약은 전문가 상담을 권장드립니다.")

**에러 응답**

- `DOCUMENT_NOT_FOUND`: 문서를 찾을 수 없음
- `VALIDATION_ERROR`: 요청 값 검증 실패
- `LLM_UPSTREAM_ERROR`: 분석 엔진 응답 불안정
- `JSON_REPAIR_FAILED`: 분석 결과 형식 오류
- `RATE_LIMITED`: 요청 한도 초과

---

#### GET /api/v1/analyses/{id}

분석 결과를 조회합니다.

**요청**

- 경로 파라미터:
  - `id` (UUID, 필수): 분석 결과 ID

**응답**

POST /api/v1/analyses와 동일한 형식

**에러 응답**

- `DOCUMENT_NOT_FOUND`: 분석 결과를 찾을 수 없음
- `VALIDATION_ERROR`: 잘못된 UUID 형식
- `RATE_LIMITED`: 요청 한도 초과

---

#### GET /api/v1/analyses/documents/{documentId}

특정 문서에 대한 모든 분석 결과를 조회합니다.

**요청**

- 경로 파라미터:
  - `documentId` (UUID, 필수): 문서 ID

**응답**

```json
{
  "success": true,
  "data": [
    {
      "analysisId": "660e8400-e29b-41d4-a716-446655440000",
      "overallSummary": { ... },
      "items": [ ... ],
      "negotiationSuggestions": [ ... ],
      "disclaimer": "..."
    }
  ]
}
```

**에러 응답**

- `VALIDATION_ERROR`: 잘못된 UUID 형식
- `RATE_LIMITED`: 요청 한도 초과

---

#### GET /api/v1/analyses/history

분석 이력을 조회합니다.

**요청**

- 쿼리 파라미터:
  - `page` (int, 선택): 페이지 번호 (기본값: 0)
  - `size` (int, 선택): 페이지 크기 (기본값: 20)

**응답**

```json
{
  "success": true,
  "data": [
    {
      "analysisId": "660e8400-e29b-41d4-a716-446655440000",
      "overallSummary": { ... },
      "items": [ ... ],
      "negotiationSuggestions": [ ... ],
      "disclaimer": "..."
    }
  ]
}
```

**에러 응답**

- `RATE_LIMITED`: 요청 한도 초과

---

## 데이터 타입

### UUID

표준 UUID 형식 (예: `550e8400-e29b-41d4-a716-446655440000`)

### Instant

ISO 8601 형식의 타임스탬프 (예: `2024-01-01T00:00:00Z`)

### ContractType

계약서 유형 열거형:
- `FREELANCER`: 프리랜서 계약
- `EMPLOYMENT`: 정규직 계약
- `PART_TIME`: 파트타임 계약
- `LEASE`: 임대차 계약
- `NDA`: 기밀유지계약
- `OTHER`: 기타

### UserProfile

사용자 프로필 열거형:
- `STUDENT`: 학생
- `ENTRY_LEVEL`: 신입
- `FREELANCER`: 프리랜서
- `INDIVIDUAL_BUSINESS`: 개인사업자
- `GENERAL_CONSUMER`: 일반 소비자

### AnalysisItem Label

분석 항목 레이블:
- `WARNING`: 경고 (주의 필요)
- `CHECK`: 확인 필요
- `OK`: 정상

---

## 제한 사항

- 파일 크기: 최대 10MB
- 지원 파일 형식: PDF만 지원 (이미지 OCR 미지원)
- Rate Limit: 분당 30회 요청
- 언어: 한국어 (`ko-KR`)만 지원

---

## 예제

### 문서 업로드 및 분석 전체 플로우

1. 문서 업로드

```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -F "file=@contract.pdf"
```

2. 텍스트 추출

```bash
curl -X POST http://localhost:8080/api/v1/documents/{documentId}/extract
```

3. 분석 요청

```bash
curl -X POST http://localhost:8080/api/v1/analyses \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "contractType": "FREELANCER",
    "userProfile": "STUDENT",
    "language": "ko-KR"
  }'
```

4. 분석 결과 조회

```bash
curl http://localhost:8080/api/v1/analyses/{analysisId}
```
