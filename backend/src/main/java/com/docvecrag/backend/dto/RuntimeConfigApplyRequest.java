package com.docvecrag.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class RuntimeConfigApplyRequest {

    @NotBlank
    private String kbName;

    private Cloudflare cloudflare = new Cloudflare();
    private Generation generation = new Generation();

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
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

    public static class Generation {
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
