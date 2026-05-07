package com.docvecrag.backend.service.vector;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.config.Defaults;
import com.docvecrag.backend.model.IndexedChunk;
import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CloudflareVectorizeClientStub implements VectorStoreClient {
    private final AppProperties appProperties;
    private final Map<String, List<IndexedChunk>> indexStore = new ConcurrentHashMap<>();

    public CloudflareVectorizeClientStub(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void upsert(String kbName, List<IndexedChunk> chunks) {
        indexStore.put(buildStoreKey(kbName), new ArrayList<>(chunks));
    }

    @Override
    public List<RetrievedChunk> queryTopK(String kbName, List<Float> queryEmbedding, int topK,
            Map<String, String> metadataFilter) {
        List<IndexedChunk> indexed = indexStore.getOrDefault(buildStoreKey(kbName), List.of());

        return indexed.stream()
                .filter(item -> matchesFilter(item, metadataFilter))
                .map(item -> new RetrievedChunk(
                        item.getChunkId(),
                        item.getSourceDocId(),
                        item.getIngestedAt(),
                        item.getText(),
                        cosine(queryEmbedding, item.getEmbedding()),
                        item.getMetadata()))
                .sorted(Comparator.comparingDouble(RetrievedChunk::getScore).reversed()
                        .thenComparing(RetrievedChunk::getChunkId))
                .limit(Math.max(1, topK))
                .toList();
    }

    private boolean matchesFilter(IndexedChunk item, Map<String, String> metadataFilter) {
        if (metadataFilter == null || metadataFilter.isEmpty()) {
            return true;
        }

        String chunkId = metadataFilter.get("chunkId");
        if (chunkId != null && !chunkId.equals(item.getChunkId())) {
            return false;
        }

        String sourceDocId = metadataFilter.get("sourceDocId");
        if (sourceDocId != null && !sourceDocId.equals(item.getSourceDocId())) {
            return false;
        }

        Map<String, String> metadata = item.getMetadata();
        for (Map.Entry<String, String> entry : metadataFilter.entrySet()) {
            String key = entry.getKey();
            String expected = entry.getValue();
            if (key == null || expected == null) {
                continue;
            }
            if ("chunkId".equals(key) || "sourceDocId".equals(key)) {
                continue;
            }
            String actual = metadata == null ? null : metadata.get(key);
            if (actual == null || !expected.equals(actual)) {
                return false;
            }
        }

        return true;
    }

    private double cosine(List<Float> left, List<Float> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0.0;
        }

        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.size(); i++) {
            float l = left.get(i);
            float r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String buildStoreKey(String kbName) {
        String normalizedKbName = (kbName == null || kbName.isBlank()) ? Defaults.KB_NAME : kbName.trim();
        String indexName = appProperties.getVectorize().getIndexName();
        String namespace = appProperties.getVectorize().getNamespace();

        SessionRuntimeConfig config = SessionRuntimeConfigContextHolder.getCurrent();
        if (config != null && config.getCloudflare() != null) {
            String runtimeIndexName = config.getCloudflare().getVectorizeIndexName();
            String runtimeNamespace = config.getCloudflare().getVectorizeNamespace();
            if (runtimeIndexName != null && !runtimeIndexName.isBlank()) {
                indexName = runtimeIndexName.trim();
            }
            if (runtimeNamespace != null && !runtimeNamespace.isBlank()) {
                namespace = runtimeNamespace.trim();
            }
        }

        return indexName + "::" + namespace + "::" + normalizedKbName;
    }
}
