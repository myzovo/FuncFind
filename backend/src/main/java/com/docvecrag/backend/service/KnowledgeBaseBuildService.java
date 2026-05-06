package com.docvecrag.backend.service;

import com.docvecrag.backend.dto.KnowledgeBaseBuildRequest;
import com.docvecrag.backend.dto.KnowledgeBaseBuildResponse;
import com.docvecrag.backend.model.ChunkRecord;
import com.docvecrag.backend.model.IndexedChunk;
import com.docvecrag.backend.model.StoredDocument;
import com.docvecrag.backend.service.chunk.TwoStageChunkingService;
import com.docvecrag.backend.service.embedding.EmbeddingBinding;
import com.docvecrag.backend.service.embedding.EmbeddingService;
import com.docvecrag.backend.service.storage.RawTextStore;
import com.docvecrag.backend.service.vector.VectorStoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseBuildService {

    private final RawTextStore rawTextStore;
    private final TwoStageChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorStoreClient vectorStoreClient;

    public KnowledgeBaseBuildService(RawTextStore rawTextStore,
            TwoStageChunkingService chunkingService,
            EmbeddingService embeddingService,
            VectorStoreClient vectorStoreClient) {
        this.rawTextStore = rawTextStore;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorStoreClient = vectorStoreClient;
    }

    public KnowledgeBaseBuildResponse build(KnowledgeBaseBuildRequest request) {
        List<StoredDocument> docs = rawTextStore.findByKbName(request.getKbName());
        if (docs.isEmpty()) {
            throw new IllegalStateException("No documents found in kbName=" + request.getKbName());
        }

        EmbeddingBinding binding = embeddingService.bindForIndexing(request.getEmbeddingModel());

        List<ChunkRecord> chunks = chunkingService.chunkDocuments(
                docs,
                request.getKbName(),
                request.getChunkSize(),
                request.getSemanticThreshold());

        List<IndexedChunk> indexed = new ArrayList<>();
        for (ChunkRecord chunk : chunks) {
            indexed.add(new IndexedChunk(
                    chunk.getChunkId(),
                    chunk.getSourceDocId(),
                    chunk.getIngestedAt(),
                    chunk.getText(),
                    embeddingService.embedForIndexing(chunk.getText(), request.getEmbeddingModel()),
                    Map.of("sourceType", "document")));
        }

        vectorStoreClient.upsert(request.getKbName(), indexed);

        KnowledgeBaseBuildResponse response = new KnowledgeBaseBuildResponse();
        response.setKbName(request.getKbName());
        response.setDocumentCount(docs.size());
        response.setIndexedChunkCount(indexed.size());
        response.setEmbeddingModel(binding.getModelId());
        response.setEmbeddingDimension(binding.getDimension());
        response.setStatus("SUCCESS");
        response.setMessage("Knowledge base built and indexed to Cloudflare Vectorize boundary client.");
        return response;
    }
}
