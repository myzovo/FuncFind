package com.docvecrag.backend.service.embedding;

public class EmbeddingBinding {
    private final String modelId;
    private final int dimension;

    public EmbeddingBinding(String modelId, int dimension) {
        this.modelId = modelId;
        this.dimension = dimension;
    }

    public String getModelId() {
        return modelId;
    }

    public int getDimension() {
        return dimension;
    }
}
