package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.model.StoredDocument;

import java.util.List;

public interface RawTextStore {
    StoredDocument save(String kbName, String filename, String rawText);

    StoredDocument save(StoredDocument document);

    List<StoredDocument> findByKbName(String kbName);
}
