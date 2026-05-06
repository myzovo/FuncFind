package com.docvecrag.backend.service;

import com.docvecrag.backend.model.StoredDocument;
import com.docvecrag.backend.service.preprocess.PreprocessResult;
import com.docvecrag.backend.service.preprocess.TextPreprocessor;
import com.docvecrag.backend.service.storage.RawTextStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentIngestionService {

    private final RawTextStore rawTextStore;
    private final TextPreprocessor textPreprocessor;

    public DocumentIngestionService(RawTextStore rawTextStore, TextPreprocessor textPreprocessor) {
        this.rawTextStore = rawTextStore;
        this.textPreprocessor = textPreprocessor;
    }

    public StoredDocument ingest(MultipartFile file, String kbName) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        String extracted = extractText(file);
        PreprocessResult preprocessResult = textPreprocessor.preprocess(extracted);
        String normalizedKb = (kbName == null || kbName.isBlank()) ? "default-kb" : kbName.trim();

        return rawTextStore.save(normalizedKb, safeFilename(file), preprocessResult.getCleanedText());
    }

    private String extractText(MultipartFile file) throws IOException {
        String filename = safeFilename(file).toLowerCase();
        if (filename.endsWith(".txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        // Placeholder for unstructured or dedicated extraction pipeline.
        return "[extracted-placeholder] file=" + safeFilename(file)
                + "\nThis is a bootstrap extraction result. Replace with unstructured-based extraction in next step.";
    }

    private String safeFilename(MultipartFile file) {
        return file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
    }
}
