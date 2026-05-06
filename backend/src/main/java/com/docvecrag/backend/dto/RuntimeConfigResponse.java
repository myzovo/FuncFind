package com.docvecrag.backend.dto;

public class RuntimeConfigResponse {

    private String sessionId;
    private String kbName;
    private String appliedAt;
    private Cloudflare cloudflare = new Cloudflare();
    private Generation generation = new Generation();

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

    public String getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(String appliedAt) {
        this.appliedAt = appliedAt;
    }

    public Cloudflare getCloudflare() {
        return cloudflare;
    }

    public void setCloudflare(Cloudflare cloudflare) {
        this.cloudflare = cloudflare;
    }

    public Generation getGeneration() {
        return generation;
    }

    public void setGeneration(Generation generation) {
        this.generation = generation;
    }

    public static class Cloudflare {
        private String accountId;
        private String vectorizeIndexName;
        private String vectorizeNamespace;
        private String r2Bucket;
        private boolean hasApiToken;
        private String apiTokenMasked;

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
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

        public boolean isHasApiToken() {
            return hasApiToken;
        }

        public void setHasApiToken(boolean hasApiToken) {
            this.hasApiToken = hasApiToken;
        }

        public String getApiTokenMasked() {
            return apiTokenMasked;
        }

        public void setApiTokenMasked(String apiTokenMasked) {
            this.apiTokenMasked = apiTokenMasked;
        }
    }

    public static class Generation {
        private String providerType;
        private String baseUrl;
        private String generationModelId;
        private boolean hasApiKey;
        private String apiKeyMasked;

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

        public String getGenerationModelId() {
            return generationModelId;
        }

        public void setGenerationModelId(String generationModelId) {
            this.generationModelId = generationModelId;
        }

        public boolean isHasApiKey() {
            return hasApiKey;
        }

        public void setHasApiKey(boolean hasApiKey) {
            this.hasApiKey = hasApiKey;
        }

        public String getApiKeyMasked() {
            return apiKeyMasked;
        }

        public void setApiKeyMasked(String apiKeyMasked) {
            this.apiKeyMasked = apiKeyMasked;
        }
    }
}
