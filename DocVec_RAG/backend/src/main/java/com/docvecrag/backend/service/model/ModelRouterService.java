package com.docvecrag.backend.service.model;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.preprocess.PreprocessResult;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelRouterService {

    private final AppProperties appProperties;
    private final Map<String, ModelAdapter> adapters = new HashMap<>();

    private Instant healthCacheAt = Instant.EPOCH;
    private boolean defaultHealthy = false;
    private boolean fallbackHealthy = false;

    public ModelRouterService(AppProperties appProperties, List<ModelAdapter> adapterList) {
        this.appProperties = appProperties;
        for (ModelAdapter adapter : adapterList) {
            adapters.put(adapter.adapterId(), adapter);
        }
    }

    public PreprocessResult preprocess(String rawText) {
        return resolveAdapter().preprocess(rawText);
    }

    public List<Float> embed(String text, int dimension) {
        return resolveAdapter().embed(text, dimension);
    }

    public String generate(String question, List<RetrievedChunk> contexts, String generationModel) {
        return resolveAdapter().generate(question, contexts, generationModel);
    }

    public String currentAdapterId() {
        return resolveAdapter().adapterId();
    }

    public ModelRoutingStatus routingStatus() {
        refreshHealthIfNeeded();
        String preferredAdapter = getPreferredAdapterFromContext();

        ModelRoutingStatus status = new ModelRoutingStatus();
        status.setDefaultAdapter(appProperties.getModelRouting().getDefaultAdapter());
        status.setFallbackAdapter(appProperties.getModelRouting().getFallbackAdapter());
        status.setDefaultHealthy(defaultHealthy);
        status.setFallbackHealthy(fallbackHealthy);
        status.setPreferredAdapter(preferredAdapter);
        status.setSelectedAdapter(resolveAdapter(preferredAdapter).adapterId());
        return status;
    }

    private synchronized void refreshHealthIfNeeded() {
        int ttl = Math.max(1, appProperties.getModelRouting().getHealthCheckTtlSeconds());
        if (Instant.now().isBefore(healthCacheAt.plusSeconds(ttl))) {
            return;
        }

        ModelAdapter defaultAdapter = adapters.get(appProperties.getModelRouting().getDefaultAdapter());
        ModelAdapter fallbackAdapter = adapters.get(appProperties.getModelRouting().getFallbackAdapter());

        defaultHealthy = defaultAdapter != null && defaultAdapter.isHealthy();
        fallbackHealthy = fallbackAdapter != null && fallbackAdapter.isHealthy();
        healthCacheAt = Instant.now();
    }

    private ModelAdapter resolveAdapter() {
        return resolveAdapter(getPreferredAdapterFromContext());
    }

    private ModelAdapter resolveAdapter(String preferredAdapterId) {
        refreshHealthIfNeeded();

        String defaultId = appProperties.getModelRouting().getDefaultAdapter();
        String fallbackId = appProperties.getModelRouting().getFallbackAdapter();

        if (preferredAdapterId != null && !preferredAdapterId.isBlank()) {
            ModelAdapter preferredAdapter = adapters.get(preferredAdapterId);
            if (preferredAdapter != null && preferredAdapter.isHealthy()) {
                return preferredAdapter;
            }
        }

        ModelAdapter defaultAdapter = adapters.get(defaultId);
        if (defaultHealthy && defaultAdapter != null) {
            return defaultAdapter;
        }

        ModelAdapter fallbackAdapter = adapters.get(fallbackId);
        if (fallbackHealthy && fallbackAdapter != null) {
            return fallbackAdapter;
        }

        throw new IllegalStateException("No healthy model adapter available. default="
                + defaultId + ", fallback=" + fallbackId);
    }

    private String getPreferredAdapterFromContext() {
        SessionRuntimeConfig config = SessionRuntimeConfigContextHolder.getCurrent();
        if (config == null || config.getGeneration() == null) {
            return null;
        }

        String providerType = config.getGeneration().getProviderType();
        if (providerType == null || providerType.isBlank()) {
            return null;
        }

        String normalized = providerType.trim().toLowerCase();
        if (!adapters.containsKey(normalized)) {
            return null;
        }
        return normalized;
    }
}
