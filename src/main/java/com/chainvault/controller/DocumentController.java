package com.chainvault.controller;

import com.chainvault.model.Document;
import com.chainvault.model.ProvenanceRecord;
import com.chainvault.repository.DocumentRepository;
import com.chainvault.repository.ProvenanceRecordRepository;
import com.chainvault.service.BlockchainLedgerService;
import com.chainvault.service.QRCodeService;
import com.chainvault.util.HashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final ProvenanceRecordRepository provenanceRepository;
    private final BlockchainLedgerService ledgerService;
    private final QRCodeService qrCodeService;

    @Value("${file.upload-dir:../uploads}")
    private String uploadDir;

    public DocumentController(DocumentRepository documentRepository,
                              ProvenanceRecordRepository provenanceRepository,
                              BlockchainLedgerService ledgerService,
                              QRCodeService qrCodeService) {
        this.documentRepository = documentRepository;
        this.provenanceRepository = provenanceRepository;
        this.ledgerService = ledgerService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("issuer") String issuer) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "File cannot be empty"));
            }

            byte[] bytes = file.getBytes();
            String sha256Hash = HashUtil.calculateSHA256(bytes);

            Optional<ProvenanceRecord> existing = provenanceRepository.findBySha256Hash(sha256Hash);
            if (existing.isPresent()) {
                return ResponseEntity.status(409).body(Map.of("message", "Document with identical content/hash is already registered"));
            }

            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String savedFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, savedFileName);
            Files.write(filePath, bytes);

            Document document = new Document(title, file.getOriginalFilename(), filePath.toString(), sha256Hash, issuer);
            document = documentRepository.save(document);

            String txHash = ledgerService.recordToLedger(document.getId(), sha256Hash, issuer, "ACTIVE");

            ProvenanceRecord record = new ProvenanceRecord(document.getId(), sha256Hash, "ACTIVE", txHash);
            provenanceRepository.save(record);

            String qrData = "http://localhost:5173/verify?hash=" + sha256Hash;
            String qrCodeBase64 = qrCodeService.generateQRCodeBase64(qrData, 250, 250);

            Map<String, Object> response = new HashMap<>();
            response.put("documentId", document.getId());
            response.put("title", document.getTitle());
            response.put("sha256Hash", sha256Hash);
            response.put("status", "ACTIVE");
            response.put("transactionRef", txHash);
            response.put("qrCode", qrCodeBase64);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error storing file: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-file")
    public ResponseEntity<?> verifyFile(@RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String calculatedHash = HashUtil.calculateSHA256(bytes);

            Optional<ProvenanceRecord> recordOpt = provenanceRepository.findBySha256Hash(calculatedHash);

            if (recordOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "NOT_REGISTERED",
                    "calculatedHash", calculatedHash,
                    "message", "No matching fingerprint found on the blockchain ledger."
                ));
            }

            ProvenanceRecord record = recordOpt.get();
            Optional<Document> docOpt = documentRepository.findById(record.getDocumentId());
            Document doc = docOpt.orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("calculatedHash", calculatedHash);
            response.put("registeredHash", record.getSha256Hash());
            response.put("status", record.getStatus().equals("ACTIVE") ? "AUTHENTIC" : "REVOKED");
            response.put("transactionRef", record.getTransactionRef());
            response.put("documentTitle", doc != null ? doc.getTitle() : "Unknown");
            response.put("issuer", doc != null ? doc.getIssuerUsername() : "Unknown");
            response.put("registeredAt", record.getTimestamp());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("message", "File verification failed"));
        }
    }

    @GetMapping("/verify/{hash}")
    public ResponseEntity<?> verifyByHash(@PathVariable("hash") String hash) {
        Optional<ProvenanceRecord> recordOpt = provenanceRepository.findBySha256Hash(hash);

        if (recordOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "status", "NOT_REGISTERED",
                "calculatedHash", hash,
                "message", "No document registered with this hash."
            ));
        }

        ProvenanceRecord record = recordOpt.get();
        Optional<Document> docOpt = documentRepository.findById(record.getDocumentId());
        Document doc = docOpt.orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("registeredHash", record.getSha256Hash());
        response.put("status", record.getStatus().equals("ACTIVE") ? "AUTHENTIC" : "REVOKED");
        response.put("transactionRef", record.getTransactionRef());
        response.put("documentTitle", doc != null ? doc.getTitle() : "Unknown");
        response.put("issuer", doc != null ? doc.getIssuerUsername() : "Unknown");
        response.put("registeredAt", record.getTimestamp());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/revoke")
    public ResponseEntity<?> revokeDocument(@PathVariable("id") Long id) {
        Optional<Document> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Document doc = docOpt.get();
        Optional<ProvenanceRecord> recordOpt = provenanceRepository.findBySha256Hash(doc.getSha256Hash());

        if (recordOpt.isPresent()) {
            ProvenanceRecord record = recordOpt.get();
            record.setStatus("REVOKED");
            provenanceRepository.save(record);

            String newTxHash = ledgerService.recordToLedger(doc.getId(), doc.getSha256Hash(), doc.getIssuerUsername(), "REVOKED");
            record.setTransactionRef(newTxHash);
            provenanceRepository.save(record);

            return ResponseEntity.ok(Map.of("message", "Document revoked successfully", "revocationTx", newTxHash));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Provenance record not found"));
    }

    @GetMapping
    public ResponseEntity<?> getAllDocuments() {
        return ResponseEntity.ok(documentRepository.findAll());
    }
}