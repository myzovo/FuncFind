package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.model.StoredDocument;

import java.util.List;

public interface R2RawTextClient {
    void save(StoredDocument document);

    List<StoredDocument> findByKbName(String kbName);

    boolean isHealthy();
}
