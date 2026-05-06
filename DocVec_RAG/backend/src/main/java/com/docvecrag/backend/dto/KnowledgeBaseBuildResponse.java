package com.docvecrag.backend.dto;

public class KnowledgeBaseBuildResponse {
    private String kbName;
    private int documentCount;
    private int indexedChunkCount;
    private String embeddingModel;
    private int embeddingDimension;
    private String status;
    private String message;

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public int getIndexedChunkCount() {
        return indexedChunkCount;
    }

    public void setIndexedChunkCount(int indexedChunkCount) {
        this.indexedChunkCount = indexedChunkCount;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
