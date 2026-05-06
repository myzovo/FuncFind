package com.docvecrag.backend.service.embedding;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.service.model.ModelRouterService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private final AppProperties appProperties;
    private final ModelRouterService modelRouterService;
    private EmbeddingBinding activeBinding;

    public EmbeddingService(AppProperties appProperties, ModelRouterService modelRouterService) {
        this.appProperties = appProperties;
        this.modelRouterService = modelRouterService;

        if (appProperties.getEmbedding().getModelId() == null || appProperties.getEmbedding().getModelId().isBlank()) {
            throw new IllegalStateException("app.embedding.model-id must be configured.");
        }
        if (appProperties.getEmbedding().getDimension() <= 0) {
            throw new IllegalStateException("app.embedding.dimension must be positive.");
        }
    }

    public synchronized EmbeddingBinding bindForIndexing(String requestedModelId) {
        String runtimeModelId = appProperties.getEmbedding().getModelId();
        int runtimeDimension = appProperties.getEmbedding().getDimension();

        if (requestedModelId != null && !requestedModelId.isBlank() && !requestedModelId.equals(runtimeModelId)) {
            throw new IllegalArgumentException("Embedding model mismatch with runtime config. Requested="
                    + requestedModelId + ", configured=" + runtimeModelId);
        }

        EmbeddingBinding candidate = new EmbeddingBinding(runtimeModelId, runtimeDimension);
        if (activeBinding == null) {
            activeBinding = candidate;
            return activeBinding;
        }

        boolean sameModel = activeBinding.getModelId().equals(candidate.getModelId());
        boolean sameDimension = activeBinding.getDimension() == candidate.getDimension();
        if (!sameModel || !sameDimension) {
            throw new IllegalStateException(
                    "Embedding model/dimension must stay identical for indexing and retrieval.");
        }
        return activeBinding;
    }

    public List<Float> embedForIndexing(String text, String requestedModelId) {
        EmbeddingBinding binding = bindForIndexing(requestedModelId);
        return modelRouterService.embed(text, binding.getDimension());
    }

    public List<Float> embedForQuery(String text) {
        EmbeddingBinding binding = getActiveBinding();
        if (!binding.getModelId().equals(appProperties.getEmbedding().getModelId())
                || binding.getDimension() != appProperties.getEmbedding().getDimension()) {
            throw new IllegalStateException(
                    "Current runtime embedding config diverges from active index embedding binding.");
        }
        return modelRouterService.embed(text, binding.getDimension());
    }

    public synchronized EmbeddingBinding getActiveBinding() {
        if (activeBinding == null) {
            throw new IllegalStateException("Knowledge base is not built yet. No embedding binding available.");
        }
        return activeBinding;
    }
}
