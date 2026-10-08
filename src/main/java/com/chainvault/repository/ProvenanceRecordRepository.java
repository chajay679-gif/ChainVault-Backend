package com.chainvault.repository;

import com.chainvault.model.ProvenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProvenanceRecordRepository extends JpaRepository<ProvenanceRecord, Long> {
    Optional<ProvenanceRecord> findBySha256Hash(String sha256Hash);
}