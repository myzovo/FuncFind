package com.docvecrag.backend.model;

import java.time.Instant;

public class ChunkRecord {
    private String chunkId;
    private String sourceDocId;
    private String kbName;
    private String text;
    private Instant ingestedAt;

    public ChunkRecord() {
    }

    public ChunkRecord(String chunkId, String sourceDocId, String kbName, String text, Instant ingestedAt) {
        this.chunkId = chunkId;
        this.sourceDocId = sourceDocId;
        this.kbName = kbName;
        this.text = text;
        this.ingestedAt = ingestedAt;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getSourceDocId() {
        return sourceDocId;
    }

    public void setSourceDocId(String sourceDocId) {
        this.sourceDocId = sourceDocId;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
