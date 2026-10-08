package com.chainvault.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class BlockchainLedgerService {

    private final String ledgerFilePath = "../blockchain/ledger.json";

    public String recordToLedger(Long docId, String sha256Hash, String issuer, String status) {
        String txHash = "0x" + UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());
        
        String entry = String.format(
            "{\"txHash\":\"%s\",\"docId\":%d,\"hash\":\"%s\",\"issuer\":\"%s\",\"status\":\"%s\",\"timestamp\":\"%s\"}%n",
            txHash, docId, sha256Hash, issuer, status, Instant.now().toString()
        );

        try {
            File file = new File(ledgerFilePath);
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file, true);
            writer.write(entry);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write transaction to blockchain ledger", e);
        }

        return txHash;
    }
}