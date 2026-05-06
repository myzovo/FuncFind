package com.docvecrag.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeBaseBuildRequest {
    @NotBlank
    private String kbName;

    @Min(100)
    @Max(3000)
    private int chunkSize = 500;

    @Min(0)
    @Max(1)
    private double semanticThreshold = 0.78;

    @Min(1)
    @Max(20)
    private int topK = 6;

    private String embeddingModel = "all-MiniLM-L6-v2";

    private List<SourceDocRequest> sourceDocs = new ArrayList<>();

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public double getSemanticThreshold() {
        return semanticThreshold;
    }

    public void setSemanticThreshold(double semanticThreshold) {
        this.semanticThreshold = semanticThreshold;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<SourceDocRequest> getSourceDocs() {
        return sourceDocs;
    }

    public void setSourceDocs(List<SourceDocRequest> sourceDocs) {
        this.sourceDocs = sourceDocs;
    }
}
