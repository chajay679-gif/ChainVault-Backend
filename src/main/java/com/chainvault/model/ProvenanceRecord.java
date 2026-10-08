package com.chainvault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "provenance_records")
public class ProvenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 64, unique = true)
    private String sha256Hash;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String transactionRef;

    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    public ProvenanceRecord() {}

    public ProvenanceRecord(Long documentId, String sha256Hash, String status, String transactionRef) {
        this.documentId = documentId;
        this.sha256Hash = sha256Hash;
        this.status = status;
        this.transactionRef = transactionRef;
    }

    public Long getId() { return id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public LocalDateTime getTimestamp() { return timestamp; }
}