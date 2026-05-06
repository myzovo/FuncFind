package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.model.StoredDocument;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component("persistentRawTextStore")
public class InMemoryRawTextStore implements RawTextStore {
    private final Map<String, List<StoredDocument>> documentStore = new ConcurrentHashMap<>();

    @Override
    public StoredDocument save(String kbName, String filename, String rawText) {
        return save(new StoredDocument(
                UUID.randomUUID().toString(),
                kbName,
                filename,
                rawText,
                Instant.now()));
    }

    @Override
    public StoredDocument save(StoredDocument document) {
        StoredDocument doc = document;
        if (doc.getDocumentId() == null || doc.getDocumentId().isBlank()) {
            doc.setDocumentId(UUID.randomUUID().toString());
        }
        if (doc.getCreatedAt() == null) {
            doc.setCreatedAt(Instant.now());
        }
        if (doc.getKbName() == null || doc.getKbName().isBlank()) {
            doc.setKbName("default-kb");
        }
        documentStore.computeIfAbsent(doc.getKbName(), ignored -> new ArrayList<>()).add(doc);
        return doc;
    }

    @Override
    public List<StoredDocument> findByKbName(String kbName) {
        return new ArrayList<>(documentStore.getOrDefault(kbName, List.of()));
    }
}
