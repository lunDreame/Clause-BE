package com.clause.app.domain.document.repo;

import com.clause.app.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findByTextSha256(String sha256);
}

