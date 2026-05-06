package com.docvecrag.backend.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class IndexedChunk {
    private String chunkId;
    private String sourceDocId;
    private Instant ingestedAt;
    private String text;
    private List<Float> embedding;
    private Map<String, String> metadata;

    public IndexedChunk() {
    }

    public IndexedChunk(String chunkId, String sourceDocId, Instant ingestedAt, String text, List<Float> embedding,
            Map<String, String> metadata) {
        this.chunkId = chunkId;
        this.sourceDocId = sourceDocId;
        this.ingestedAt = ingestedAt;
        this.text = text;
        this.embedding = embedding;
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

    public List<Float> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
