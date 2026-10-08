package com.chainvault.repository;

import com.chainvault.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findBySha256Hash(String sha256Hash);
}