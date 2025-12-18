package com.clause.app.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "contract_type", nullable = false, length = 40)
    private String contractType;

    @Column(name = "user_profile", nullable = false, length = 40)
    private String userProfile;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "overall_summary_json", columnDefinition = "TEXT")
    private String overallSummaryJson;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "negotiation_suggestions_json", columnDefinition = "TEXT")
    private String negotiationSuggestionsJson;

    @Column(name = "disclaimer", length = 400)
    private String disclaimer;

    @Column(name = "rule_triggers_json", columnDefinition = "TEXT")
    private String ruleTriggersJson;

    @Column(name = "llm_model", length = 100)
    private String llmModel;

    @Column(name = "llm_raw_json", columnDefinition = "TEXT")
    private String llmRawJson;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

