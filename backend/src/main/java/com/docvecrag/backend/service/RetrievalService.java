package com.docvecrag.backend.service;

import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.vector.VectorStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final VectorStoreClient vectorStoreClient;

    public RetrievalService(VectorStoreClient vectorStoreClient) {
        this.vectorStoreClient = vectorStoreClient;
    }

    public List<RetrievedChunk> retrieve(String kbName, List<Float> queryEmbedding, int topK,
            Map<String, String> metadataFilter) {
        log.debug("Retrieving: kb={}, topK={}", kbName, topK);
        List<RetrievedChunk> results = vectorStoreClient.queryTopK(kbName, queryEmbedding, topK, metadataFilter);
        log.debug("Retrieved {} chunks from kb={}", results.size(), kbName);
        return results;
    }
}
