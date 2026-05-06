package com.docvecrag.backend.service.session;

import java.time.Instant;

public class SessionRuntimeConfig {
    private String sessionId;
    private String kbName;
    private Instant appliedAt;
    private final CloudflareConfig cloudflare = new CloudflareConfig();
    private final GenerationConfig generation = new GenerationConfig();

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public CloudflareConfig getCloudflare() {
        return cloudflare;
    }

    public GenerationConfig getGeneration() {
        return generation;
    }

    public static class CloudflareConfig {
        private String accountId;
        private String apiToken;
        private String vectorizeIndexName;
        private String vectorizeNamespace;
        private String r2Bucket;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getVectorizeIndexName() {
            return vectorizeIndexName;
        }

        public void setVectorizeIndexName(String vectorizeIndexName) {
            this.vectorizeIndexName = vectorizeIndexName;
        }

        public String getVectorizeNamespace() {
            return vectorizeNamespace;
        }

        public void setVectorizeNamespace(String vectorizeNamespace) {
            this.vectorizeNamespace = vectorizeNamespace;
        }

        public String getR2Bucket() {
            return r2Bucket;
        }

        public void setR2Bucket(String r2Bucket) {
            this.r2Bucket = r2Bucket;
        }
    }

    public static class GenerationConfig {
        private String providerType;
        private String baseUrl;
        private String apiKey;
        private String generationModelId;

        public String getProviderType() {
            return providerType;
        }

        public void setProviderType(String providerType) {
            this.providerType = providerType;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getGenerationModelId() {
            return generationModelId;
        }

        public void setGenerationModelId(String generationModelId) {
            this.generationModelId = generationModelId;
        }
    }
}
