package com.docvecrag.backend.service;

import com.docvecrag.backend.config.Defaults;
import com.docvecrag.backend.dto.ChatRequest;
import com.docvecrag.backend.dto.ChatResponse;
import com.docvecrag.backend.dto.ContextChunkResponse;
import com.docvecrag.backend.dto.LocationHintResponse;
import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.embedding.EmbeddingService;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final AnswerGenerationService answerGenerationService;

    public ChatService(EmbeddingService embeddingService,
            RetrievalService retrievalService,
            AnswerGenerationService answerGenerationService) {
        this.embeddingService = embeddingService;
        this.retrievalService = retrievalService;
        this.answerGenerationService = answerGenerationService;
    }

    public ChatResponse chat(ChatRequest request) {
        log.info("Chat request: kb={}, topK={}, model={}", request.getKbName(), request.getTopK(), request.getGenerationModel());
        List<Float> queryEmbedding;
        List<RetrievedChunk> chunks;

        try {
            queryEmbedding = embeddingService.embedForQuery(request.getQuestion());
            chunks = retrievalService.retrieve(
                    request.getKbName(),
                    queryEmbedding,
                    request.getTopK(),
                    Map.of());
            log.info("Retrieved {} chunks for kb={}", chunks.size(), request.getKbName());
        } catch (IllegalStateException e) {
            log.warn("KB not ready for chat: {}", e.getMessage());
            // 知识库未构建时，返回友好提示而不是抛出异常
            ChatResponse response = new ChatResponse();
            response.setKbName(request.getKbName());
            response.setGenerationModel(request.getGenerationModel());
            response.setRetrievedCount(0);
            response.setAnswer("⚠️ **知识库未就绪**\n\n" + e.getMessage() + "\n\n请先上传文档并构建知识库后再进行问答。");
            response.setAiGenerated(false);
            response.setFallbackReason(e.getMessage());
            response.setContextChunks(new ArrayList<>());
            response.setLocations(new ArrayList<>());
            return response;
        }

        String generationModel = resolveGenerationModel(request.getGenerationModel());

        String answer = answerGenerationService.generate(
                request.getQuestion(),
                chunks,
                generationModel);

        ChatResponse response = new ChatResponse();
        response.setKbName(request.getKbName());
        response.setGenerationModel(generationModel);
        response.setRetrievedCount(chunks.size());
        response.setAnswer(answer);

        // 检测是否是 AI 生成的回答
        if (answer != null) {
            // 检测新的 fallback 标记
            if (answer.contains("⚠️ **AI 未参与回答生成**")) {
                response.setAiGenerated(false);
                if (answer.contains("Workers AI")) {
                    response.setFallbackReason("Workers AI API 调用失败或未配置");
                } else {
                    response.setFallbackReason("External LLM API 调用失败或未配置");
                }
            }
            // 检测旧的占位桩格式（未真正调用 LLM）
            else if (answer.contains("[adapter:") && answer.contains("[model:")
                    && answer.contains("基于检索上下文生成回答")) {
                response.setAiGenerated(false);
                response.setFallbackReason("使用了旧的占位桩代码，LLM 未真正调用");
            }
        }

        List<ContextChunkResponse> contextBlocks = new ArrayList<>();
        List<LocationHintResponse> locations = new ArrayList<>();
        Set<String> locationKeys = new HashSet<>();
        for (RetrievedChunk chunk : chunks) {
            ContextChunkResponse block = new ContextChunkResponse();
            block.setChunkId(chunk.getChunkId());
            block.setSourceDocId(chunk.getSourceDocId());
            block.setIngestedAt(chunk.getIngestedAt() == null ? null : chunk.getIngestedAt().toString());
            block.setScore(chunk.getScore());
            block.setText(chunk.getText());

            Map<String, String> metadata = chunk.getMetadata();
            if (metadata != null) {
                block.setPageUrl(metadata.get("pageUrl"));
                block.setPageTitle(metadata.get("pageTitle"));
                block.setElementText(metadata.get("elementText"));
                block.setElementType(metadata.get("elementType"));
                block.setSelector(metadata.get("selector"));
                block.setHref(metadata.get("href"));
                block.setContext(metadata.get("context"));
                block.setEvidence(metadata.get("evidence"));
                block.setContextText(metadata.get("contextText"));
                block.setNearestHeading(metadata.get("nearestHeading"));
                block.setTag(metadata.get("tag"));
                block.setRole(metadata.get("role"));
                block.setXpath(metadata.get("xpath"));
            }
            contextBlocks.add(block);

            if (metadata != null) {
                String pageUrl = metadata.get("pageUrl");
                String selector = metadata.get("selector");
                if (hasText(pageUrl) || hasText(selector)) {
                    String key = (pageUrl == null ? "" : pageUrl) + "||" + (selector == null ? "" : selector);
                    if (locationKeys.add(key)) {
                        LocationHintResponse hint = new LocationHintResponse();
                        hint.setPageUrl(pageUrl);
                        hint.setSelector(selector);
                        hint.setEvidence(resolveEvidence(metadata, chunk.getText()));
                        if (metadata != null) {
                            hint.setXpath(metadata.get("xpath"));
                            hint.setElementText(metadata.get("elementText"));
                        }
                        locations.add(hint);
                    }
                }
            }
        }
        response.setContextChunks(contextBlocks);
        response.setLocations(locations);

        return response;
    }

    private String resolveGenerationModel(String requestedModel) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return requestedModel;
        }

        SessionRuntimeConfig config = SessionRuntimeConfigContextHolder.getCurrent();
        if (config == null || config.getGeneration() == null) {
            return Defaults.GENERATION_MODEL;
        }

        String runtimeModel = config.getGeneration().getGenerationModelId();
        if (runtimeModel == null || runtimeModel.isBlank()) {
            return Defaults.GENERATION_MODEL;
        }
        return runtimeModel;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveEvidence(Map<String, String> metadata, String fallback) {
        if (metadata == null) {
            return fallback;
        }
        String evidence = metadata.get("evidence");
        if (hasText(evidence)) {
            return evidence;
        }
        return fallback;
    }
}
