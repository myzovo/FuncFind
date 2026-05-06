package com.docvecrag.backend.service.embedding;

import java.util.List;

public interface EmbeddingProvider {
    String providerId();

    String modelId();

    int dimension();

    List<Float> embed(String text);
}
