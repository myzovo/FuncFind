package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.model.StoredDocument;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Primary
@Component
public class R2FallbackRawTextStore implements RawTextStore {

    private final AppProperties appProperties;
    private final R2RawTextClient r2RawTextClient;
    private final RawTextStore persistentRawTextStore;
    private final StorageFallbackEventService fallbackEventService;

    public R2FallbackRawTextStore(AppProperties appProperties,
            R2RawTextClient r2RawTextClient,
            @Qualifier("persistentRawTextStore") RawTextStore persistentRawTextStore,
            StorageFallbackEventService fallbackEventService) {
        this.appProperties = appProperties;
        this.r2RawTextClient = r2RawTextClient;
        this.persistentRawTextStore = persistentRawTextStore;
        this.fallbackEventService = fallbackEventService;
    }

    @Override
    public StoredDocument save(String kbName, String filename, String rawText) {
        StoredDocument document = new StoredDocument(
                UUID.randomUUID().toString(),
                normalizeKbName(kbName),
                filename,
                rawText,
                Instant.now());
        return save(document);
    }

    @Override
    public StoredDocument save(StoredDocument document) {
        StoredDocument doc = normalizeDocument(document);
        String targetR2Bucket = resolveTargetR2Bucket();

        if (useR2AsPrimary()) {
            try {
                r2RawTextClient.save(doc);
                return doc;
            } catch (Exception ex) {
                StoredDocument fallbackSaved = persistentRawTextStore.save(doc);
                fallbackEventService.recordFallback(fallbackSaved, ex.getMessage(), targetR2Bucket);
                return fallbackSaved;
            }
        }

        StoredDocument fallbackSaved = persistentRawTextStore.save(doc);
        fallbackEventService.recordFallback(
                fallbackSaved,
                "R2 is not configured as primary storage.",
                targetR2Bucket);
        return fallbackSaved;
    }

    @Override
    public List<StoredDocument> findByKbName(String kbName) {
        String normalizedKbName = normalizeKbName(kbName);
        Map<String, StoredDocument> merged = new LinkedHashMap<>();

        if (useR2AsPrimary()) {
            try {
                for (StoredDocument doc : r2RawTextClient.findByKbName(normalizedKbName)) {
                    merged.put(doc.getDocumentId(), doc);
                }
            } catch (Exception ignored) {
                // Fallback-only read remains available when R2 is down.
            }
        }

        for (StoredDocument doc : persistentRawTextStore.findByKbName(normalizedKbName)) {
            merged.putIfAbsent(doc.getDocumentId(), doc);
        }

        return new ArrayList<>(merged.values());
    }

    private boolean useR2AsPrimary() {
        String primary = appProperties.getStorage().getPrimary();
        return primary != null
                && "r2".equalsIgnoreCase(primary)
                && appProperties.getStorage().getR2().isEnabled();
    }

    private StoredDocument normalizeDocument(StoredDocument document) {
        StoredDocument doc = document;
        if (doc.getDocumentId() == null || doc.getDocumentId().isBlank()) {
            doc.setDocumentId(UUID.randomUUID().toString());
        }
        if (doc.getKbName() == null || doc.getKbName().isBlank()) {
            doc.setKbName("default-kb");
        }
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(Instant.now());
        }
        return doc;
    }

    private String normalizeKbName(String kbName) {
        return (kbName == null || kbName.isBlank()) ? "default-kb" : kbName.trim();
    }

    private String resolveTargetR2Bucket() {
        SessionRuntimeConfig config = SessionRuntimeConfigContextHolder.getCurrent();
        if (config != null
                && config.getCloudflare() != null
                && config.getCloudflare().getR2Bucket() != null
                && !config.getCloudflare().getR2Bucket().isBlank()) {
            return config.getCloudflare().getR2Bucket().trim();
        }
        return appProperties.getStorage().getR2().getBucket();
    }
}
