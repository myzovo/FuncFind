package com.docvecrag.backend.service;

import com.docvecrag.backend.dto.ChatRequest;
import com.docvecrag.backend.dto.ChatResponse;
import com.docvecrag.backend.dto.ContextChunkResponse;
import com.docvecrag.backend.dto.LocationHintResponse;
import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.embedding.EmbeddingService;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatService {

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
        List<Float> queryEmbedding = embeddingService.embedForQuery(request.getQuestion());
        List<RetrievedChunk> chunks = retrievalService.retrieve(
                request.getKbName(),
                queryEmbedding,
                request.getTopK(),
                Map.of());

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
            return "gpt-4o-mini";
        }

        String runtimeModel = config.getGeneration().getGenerationModelId();
        if (runtimeModel == null || runtimeModel.isBlank()) {
            return "gpt-4o-mini";
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
