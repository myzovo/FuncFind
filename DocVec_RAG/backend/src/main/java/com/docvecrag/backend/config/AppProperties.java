package com.docvecrag.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String embeddingProvider = "workers-ai";
    private final Embedding embedding = new Embedding();
    private final Vectorize vectorize = new Vectorize();
    private final Storage storage = new Storage();
    private final ModelRouting modelRouting = new ModelRouting();
    private final WorkersAi workersAi = new WorkersAi();
    private final ExternalModelService externalModelService = new ExternalModelService();

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public Vectorize getVectorize() {
        return vectorize;
    }

    public Storage getStorage() {
        return storage;
    }

    public ModelRouting getModelRouting() {
        return modelRouting;
    }

    public WorkersAi getWorkersAi() {
        return workersAi;
    }

    public ExternalModelService getExternalModelService() {
        return externalModelService;
    }

    public static class Embedding {
        private String modelId = "all-MiniLM-L6-v2";
        private int dimension = 384;

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public int getDimension() {
            return dimension;
        }

        public void setDimension(int dimension) {
            this.dimension = dimension;
        }
    }

    public static class Vectorize {
        private String indexName = "docvec-default";
        private String namespace = "default";

        public String getIndexName() {
            return indexName;
        }

        public void setIndexName(String indexName) {
            this.indexName = indexName;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }

    public static class Storage {
        private String rawTextMode = "r2";
        private String primary = "r2";
        private String fallback = "persistent";
        private final R2 r2 = new R2();
        private final Replay replay = new Replay();

        public String getRawTextMode() {
            return rawTextMode;
        }

        public void setRawTextMode(String rawTextMode) {
            this.rawTextMode = rawTextMode;
        }

        public String getPrimary() {
            return primary;
        }

        public void setPrimary(String primary) {
            this.primary = primary;
        }

        public String getFallback() {
            return fallback;
        }

        public void setFallback(String fallback) {
            this.fallback = fallback;
        }

        public R2 getR2() {
            return r2;
        }

        public Replay getReplay() {
            return replay;
        }
    }

    public static class R2 {
        private boolean enabled = true;
        private boolean simulateFailure = false;
        private String bucket = "docvec-raw";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSimulateFailure() {
            return simulateFailure;
        }

        public void setSimulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
    }

    public static class Replay {
        private int batchSize = 100;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class ModelRouting {
        private String defaultAdapter = "workers-ai";
        private String fallbackAdapter = "external";
        private int healthCheckTtlSeconds = 10;

        public String getDefaultAdapter() {
            return defaultAdapter;
        }

        public void setDefaultAdapter(String defaultAdapter) {
            this.defaultAdapter = defaultAdapter;
        }

        public String getFallbackAdapter() {
            return fallbackAdapter;
        }

        public void setFallbackAdapter(String fallbackAdapter) {
            this.fallbackAdapter = fallbackAdapter;
        }

        public int getHealthCheckTtlSeconds() {
            return healthCheckTtlSeconds;
        }

        public void setHealthCheckTtlSeconds(int healthCheckTtlSeconds) {
            this.healthCheckTtlSeconds = healthCheckTtlSeconds;
        }
    }

    public static class WorkersAi {
        private boolean enabled = true;
        private boolean simulateFailure = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSimulateFailure() {
            return simulateFailure;
        }

        public void setSimulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
        }
    }

    public static class ExternalModelService {
        private boolean enabled = true;
        private boolean simulateFailure = false;
        private String baseUrl = "http://localhost:9000";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSimulateFailure() {
            return simulateFailure;
        }

        public void setSimulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
