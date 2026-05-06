package com.docvecrag.backend.service.vector;

import com.docvecrag.backend.model.IndexedChunk;
import com.docvecrag.backend.model.RetrievedChunk;

import java.util.List;
import java.util.Map;

public interface VectorStoreClient {
    void upsert(String kbName, List<IndexedChunk> chunks);

    List<RetrievedChunk> queryTopK(String kbName, List<Float> queryEmbedding, int topK,
            Map<String, String> metadataFilter);
}
