package com.docvecrag.backend.service.embedding;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class WorkersAiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(WorkersAiEmbeddingProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final AppProperties appProperties;

    public WorkersAiEmbeddingProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String providerId() {
        return "workers-ai";
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
        // Resolve Cloudflare credentials from session config
        String accountId = null;
        String apiToken = null;

        SessionRuntimeConfig sessionConfig = SessionRuntimeConfigContextHolder.getCurrent();
        if (sessionConfig != null && sessionConfig.getCloudflare() != null) {
            SessionRuntimeConfig.CloudflareConfig cf = sessionConfig.getCloudflare();
            if (cf.getAccountId() != null && !cf.getAccountId().isBlank()) {
                accountId = cf.getAccountId().trim();
            }
            if (cf.getApiToken() != null && !cf.getApiToken().isBlank()) {
                apiToken = cf.getApiToken().trim();
            }
        }

        // If no Cloudflare credentials, fall back to deterministic vector
        if (accountId == null || apiToken == null) {
            log.warn("Workers AI Embedding: no Cloudflare credentials in session config, falling back to deterministic vector");
            return EmbeddingVectorUtil.deterministicVector(text, dimension());
        }

        // Call Cloudflare Workers AI Embedding API
        try {
            return callWorkersAiEmbedding(accountId, apiToken, modelId(), text);
        } catch (Exception e) {
            log.error("Workers AI Embedding API call failed, falling back to deterministic vector: {}", e.getMessage());
            return EmbeddingVectorUtil.deterministicVector(text, dimension());
        }
    }

    private List<Float> callWorkersAiEmbedding(String accountId, String apiToken, String model, String text) throws Exception {
        // Cloudflare Workers AI embedding endpoint: /accounts/{id}/ai/run/{model}
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + model;

        String requestBody = mapper.writeValueAsString(mapper.createObjectNode()
                .put("text", text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiToken)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Workers AI Embedding returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());

        // Workers AI response format: { "success": true, "result": { "data": [[...]] } }
        if (!root.path("success").asBoolean(false)) {
            String errors = root.path("errors").toString();
            throw new RuntimeException("Workers AI Embedding returned errors: " + errors);
        }

        JsonNode data = root.path("result").path("data");
        if (data.isArray() && !data.isEmpty()) {
            JsonNode embedding = data.get(0);
            if (embedding.isArray()) {
                List<Float> vector = new ArrayList<>(embedding.size());
                for (JsonNode val : embedding) {
                    vector.add(val.floatValue());
                }
                return vector;
            }
        }

        // Try alternative response format: { "success": true, "result": { "embedding": [...] } }
        JsonNode embedding = root.path("result").path("embedding");
        if (embedding.isArray()) {
            List<Float> vector = new ArrayList<>(embedding.size());
            for (JsonNode val : embedding) {
                vector.add(val.floatValue());
            }
            return vector;
        }

        throw new RuntimeException("Workers AI Embedding returned unexpected format");
    }
}
