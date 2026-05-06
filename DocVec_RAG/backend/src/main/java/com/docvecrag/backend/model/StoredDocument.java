package com.docvecrag.backend.model;

import java.time.Instant;

public class StoredDocument {
    private String documentId;
    private String kbName;
    private String filename;
    private String rawText;
    private Instant createdAt;

    public StoredDocument() {
    }

    public StoredDocument(String documentId, String kbName, String filename, String rawText, Instant createdAt) {
        this.documentId = documentId;
        this.kbName = kbName;
        this.filename = filename;
        this.rawText = rawText;
        this.createdAt = createdAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
