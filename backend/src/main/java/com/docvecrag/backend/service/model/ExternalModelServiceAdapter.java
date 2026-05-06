package com.docvecrag.backend.service.model;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.embedding.EmbeddingVectorUtil;
import com.docvecrag.backend.service.preprocess.PreprocessResult;
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
import java.util.List;
import java.util.StringJoiner;

@Component
public class ExternalModelServiceAdapter implements ModelAdapter {

    private static final Logger log = LoggerFactory.getLogger(ExternalModelServiceAdapter.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final AppProperties appProperties;

    public ExternalModelServiceAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String adapterId() {
        return "external";
    }

    @Override
    public boolean isHealthy() {
        return appProperties.getExternalModelService().isEnabled()
                && !appProperties.getExternalModelService().isSimulateFailure();
    }

    @Override
    public PreprocessResult preprocess(String rawText) {
        String normalized = rawText == null ? ""
                : rawText
                        .replace("\r\n", "\n")
                        .replaceAll("[\t ]+", " ")
                        .replaceAll("\n{3,}", "\n\n")
                        .trim();

        String title = "untitled";
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                title = trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
                break;
            }
        }

        return new PreprocessResult(title, normalized);
    }

    @Override
    public List<Float> embed(String text, int dimension) {
        return EmbeddingVectorUtil.deterministicVector("external:" + text, dimension);
    }

    @Override
    public String generate(String question, List<RetrievedChunk> contexts, String generationModel) {
        String model = (generationModel == null || generationModel.isBlank()) ? "gpt-4o-mini" : generationModel;

        // Resolve config: session runtime config takes priority over app properties
        String baseUrl = null;
        String apiKey = null;

        SessionRuntimeConfig sessionConfig = SessionRuntimeConfigContextHolder.getCurrent();
        if (sessionConfig != null && sessionConfig.getGeneration() != null) {
            SessionRuntimeConfig.GenerationConfig gen = sessionConfig.getGeneration();
            if (gen.getBaseUrl() != null && !gen.getBaseUrl().isBlank()) {
                baseUrl = gen.getBaseUrl().trim();
            }
            if (gen.getApiKey() != null && !gen.getApiKey().isBlank()) {
                apiKey = gen.getApiKey().trim();
            }
            if (gen.getGenerationModelId() != null && !gen.getGenerationModelId().isBlank()) {
                model = gen.getGenerationModelId().trim();
            }
        }

        // Fallback to app properties
        if (baseUrl == null) {
            baseUrl = appProperties.getExternalModelService().getBaseUrl();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:9000";
        }

        // Build system prompt with retrieved context
        String systemPrompt = buildSystemPrompt(contexts);

        // Call OpenAI-compatible Chat Completions API
        try {
            return callChatCompletions(baseUrl, apiKey, model, systemPrompt, question);
        } catch (Exception e) {
            log.error("External LLM API call failed, falling back to context summary: {}", e.getMessage());
            return buildFallbackAnswer(question, contexts);
        }
    }

    private String callChatCompletions(String baseUrl, String apiKey, String model,
                                       String systemPrompt, String question) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/v1/chat/completions";

        // Build request body
        String requestBody = mapper.writeValueAsString(mapper.createObjectNode()
                .put("model", model)
                .put("temperature", 0.3)
                .put("max_tokens", 1024)
                .set("messages", mapper.createArrayNode()
                        .add(mapper.createObjectNode()
                                .put("role", "system")
                                .put("content", systemPrompt))
                        .add(mapper.createObjectNode()
                                .put("role", "user")
                                .put("content", question))));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("LLM API returned empty choices");
        }

        String content = choices.get(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new RuntimeException("LLM API returned empty content");
        }

        return content.trim();
    }

    private String buildSystemPrompt(List<RetrievedChunk> contexts) {
        StringJoiner sj = new StringJoiner("\n\n");
        sj.add("你是一个智能助手，请根据以下检索到的上下文信息来回答用户的问题。");
        sj.add("要求：");
        sj.add("- 只基于提供的上下文回答，不要编造信息");
        sj.add("- 如果上下文中没有相关信息，请明确说明");
        sj.add("- 回答要简洁、准确、有条理");
        sj.add("- 使用中文回答");

        if (contexts != null && !contexts.isEmpty()) {
            sj.add("---\n检索到的上下文：");
            int limit = Math.min(8, contexts.size());
            for (int i = 0; i < limit; i++) {
                RetrievedChunk chunk = contexts.get(i);
                String text = chunk.getText();
                if (text != null && text.length() > 800) {
                    text = text.substring(0, 800) + "...";
                }
                String source = chunk.getSourceDocId() != null ? chunk.getSourceDocId() : "unknown";
                sj.add("[" + (i + 1) + "] 来源: " + source + "\n" + text);
            }
        }

        return sj.toString();
    }

    private String buildFallbackAnswer(String question, List<RetrievedChunk> contexts) {
        StringBuilder builder = new StringBuilder();
        builder.append("检索到以下相关信息（LLM 服务不可用，仅展示原始检索结果）：\n\n");

        if (contexts == null || contexts.isEmpty()) {
            builder.append("未检索到相关上下文，请先上传文档并构建知识库。");
            return builder.toString();
        }

        int limit = Math.min(5, contexts.size());
        for (int i = 0; i < limit; i++) {
            RetrievedChunk chunk = contexts.get(i);
            String preview = chunk.getText();
            if (preview != null && preview.length() > 200) {
                preview = preview.substring(0, 200) + "...";
            }
            builder.append(i + 1)
                    .append(". [")
                    .append(chunk.getSourceDocId() != null ? chunk.getSourceDocId() : "unknown")
                    .append("] ")
                    .append(preview)
                    .append("\n\n");
        }
        return builder.toString().trim();
    }
}
