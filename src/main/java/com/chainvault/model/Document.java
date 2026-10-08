package com.chainvault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false, length = 64)
    private String sha256Hash;

    @Column(nullable = false)
    private String issuerUsername;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Document() {}

    public Document(String title, String filename, String filePath, String sha256Hash, String issuerUsername) {
        this.title = title;
        this.filename = filename;
        this.filePath = filePath;
        this.sha256Hash = sha256Hash;
        this.issuerUsername = issuerUsername;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
    public String getIssuerUsername() { return issuerUsername; }
    public void setIssuerUsername(String issuerUsername) { this.issuerUsername = issuerUsername; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}