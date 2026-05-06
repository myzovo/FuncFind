package com.docvecrag.backend.service.embedding;

import com.docvecrag.backend.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExternalModelEmbeddingProvider implements EmbeddingProvider {

    private final AppProperties appProperties;

    public ExternalModelEmbeddingProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String providerId() {
        return "external";
    }

    @Override
    public String modelId() {
        return appProperties.getEmbedding().getModelId();
    }

    @Override
    public int dimension() {
        return appProperties.getEmbedding().getDimension();
    }

    @Override
    public List<Float> embed(String text) {
        return EmbeddingVectorUtil.deterministicVector("external:" + text, dimension());
    }
}
