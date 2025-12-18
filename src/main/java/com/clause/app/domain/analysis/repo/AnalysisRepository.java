package com.clause.app.domain.analysis.repo;

import com.clause.app.domain.analysis.entity.AnalysisResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<AnalysisResult, UUID> {
    List<AnalysisResult> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Pageable pageable);
    List<AnalysisResult> findByStatus(String status);
}

