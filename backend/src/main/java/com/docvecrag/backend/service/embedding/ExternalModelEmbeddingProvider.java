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
public class ExternalModelEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(ExternalModelEmbeddingProvider.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

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
        // Resolve config: session runtime config takes priority over app properties
        String baseUrl = null;
        String apiKey = null;
        String model = modelId();

        SessionRuntimeConfig sessionConfig = SessionRuntimeConfigContextHolder.getCurrent();
        if (sessionConfig != null && sessionConfig.getGeneration() != null) {
            SessionRuntimeConfig.GenerationConfig gen = sessionConfig.getGeneration();
            if (gen.getBaseUrl() != null && !gen.getBaseUrl().isBlank()) {
                baseUrl = gen.getBaseUrl().trim();
            }
            if (gen.getApiKey() != null && !gen.getApiKey().isBlank()) {
                apiKey = gen.getApiKey().trim();
            }
        }

        // Fallback to app properties
        if (baseUrl == null) {
            baseUrl = appProperties.getExternalModelService().getBaseUrl();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:9000";
        }

        // Call OpenAI-compatible Embedding API
        try {
            return callEmbeddingApi(baseUrl, apiKey, model, text);
        } catch (Exception e) {
            log.error("External Embedding API call failed, falling back to deterministic vector: {}", e.getMessage());
            return EmbeddingVectorUtil.deterministicVector("external:" + text, dimension());
        }
    }

    private List<Float> callEmbeddingApi(String baseUrl, String apiKey, String model, String text) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/v1/embeddings";

        String requestBody = mapper.writeValueAsString(mapper.createObjectNode()
                .put("model", model)
                .put("input", text));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode data = root.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            throw new RuntimeException("Embedding API returned empty data");
        }

        JsonNode embedding = data.get(0).get("embedding");
        if (embedding == null || !embedding.isArray()) {
            throw new RuntimeException("Embedding API returned invalid embedding");
        }

        List<Float> vector = new ArrayList<>(embedding.size());
        for (JsonNode val : embedding) {
            vector.add(val.floatValue());
        }
        return vector;
    }
}
