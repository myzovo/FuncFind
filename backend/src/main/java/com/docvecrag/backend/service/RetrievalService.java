package com.docvecrag.backend.service;

import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.vector.VectorStoreClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {

    private final VectorStoreClient vectorStoreClient;

    public RetrievalService(VectorStoreClient vectorStoreClient) {
        this.vectorStoreClient = vectorStoreClient;
    }

    public List<RetrievedChunk> retrieve(String kbName, List<Float> queryEmbedding, int topK,
            Map<String, String> metadataFilter) {
        return vectorStoreClient.queryTopK(kbName, queryEmbedding, topK, metadataFilter);
    }
}
