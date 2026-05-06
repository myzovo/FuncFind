package com.docvecrag.backend.model;

import java.time.Instant;
import java.util.Map;

public class RetrievedChunk {
    private String chunkId;
    private String sourceDocId;
    private Instant ingestedAt;
    private String text;
    private double score;
    private Map<String, String> metadata;

    public RetrievedChunk() {
    }

    public RetrievedChunk(String chunkId, String sourceDocId, Instant ingestedAt, String text, double score,
            Map<String, String> metadata) {
        this.chunkId = chunkId;
        this.sourceDocId = sourceDocId;
        this.ingestedAt = ingestedAt;
        this.text = text;
        this.score = score;
        this.metadata = metadata;
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

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
