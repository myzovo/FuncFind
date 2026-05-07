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
public class WorkersAiModelAdapter implements ModelAdapter {

    private static final Logger log = LoggerFactory.getLogger(WorkersAiModelAdapter.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final AppProperties appProperties;

    public WorkersAiModelAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String adapterId() {
        return "workers-ai";
    }

    @Override
    public boolean isHealthy() {
        return appProperties.getWorkersAi().isEnabled() && !appProperties.getWorkersAi().isSimulateFailure();
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
        return EmbeddingVectorUtil.deterministicVector(text, dimension);
    }

    @Override
    public String generate(String question, List<RetrievedChunk> contexts, String generationModel) {
        String model = (generationModel == null || generationModel.isBlank()) ? "@cf/meta/llama-3-8b-instruct" : generationModel;

        // Resolve config from session runtime config
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

        // If no Cloudflare credentials, fall back to external adapter behavior
        if (accountId == null || apiToken == null) {
            log.warn("Workers AI: no Cloudflare credentials in session config, falling back to context summary");
            return buildFallbackAnswer(question, contexts);
        }

        // Build system prompt with retrieved context
        String systemPrompt = buildSystemPrompt(contexts);

        // Call Cloudflare Workers AI API
        try {
            return callWorkersAi(accountId, apiToken, model, systemPrompt, question);
        } catch (Exception e) {
            log.error("Workers AI API call failed, falling back to context summary: {}", e.getMessage());
            return buildFallbackAnswer(question, contexts);
        }
    }

    private String callWorkersAi(String accountId, String apiToken, String model,
                                  String systemPrompt, String question) throws Exception {
        // Cloudflare Workers AI endpoint: /accounts/{id}/ai/run/{model}
        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + model;

        String requestBody = mapper.writeValueAsString(mapper.createObjectNode()
                .set("messages", mapper.createArrayNode()
                        .add(mapper.createObjectNode()
                                .put("role", "system")
                                .put("content", systemPrompt))
                        .add(mapper.createObjectNode()
                                .put("role", "user")
                                .put("content", question)))
                .put("stream", false));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiToken)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Workers AI returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());

        // Workers AI response format: { "success": true, "result": { "response": "..." } }
        if (!root.path("success").asBoolean(false)) {
            String errors = root.path("errors").toString();
            throw new RuntimeException("Workers AI returned errors: " + errors);
        }

        String content = root.path("result").path("response").asText("");
        if (content.isBlank()) {
            // Try OpenAI-compatible format as fallback
            JsonNode choices = root.path("result").path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                content = choices.get(0).path("message").path("content").asText("");
            }
        }

        if (content.isBlank()) {
            throw new RuntimeException("Workers AI returned empty response");
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
        builder.append("⚠️ **AI 未参与回答生成**\n\n");
        builder.append("原因：Workers AI API 调用失败或未配置，以下仅为知识库检索结果：\n\n");

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

        builder.append("---\n");
        builder.append("**调试提示**：请检查以下配置：\n");
        builder.append("- 确保 Cloudflare `accountId` 和 `apiToken` 已在设置中配置\n");
        builder.append("- 确保 Workers AI 已启用且模型可用\n");
        builder.append("- 检查后端日志获取详细错误信息");

        return builder.toString().trim();
    }
}
