package com.docvecrag.backend.service;

import com.docvecrag.backend.model.StoredDocument;
import com.docvecrag.backend.service.preprocess.PreprocessResult;
import com.docvecrag.backend.service.preprocess.TextPreprocessor;
import com.docvecrag.backend.service.storage.RawTextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

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

        String filename = safeFilename(file);
        log.info("Ingesting document: kb={}, file={}, size={}", kbName, filename, file.getSize());

        String extracted = extractText(file, filename);
        PreprocessResult preprocessResult = textPreprocessor.preprocess(extracted);
        String normalizedKb = (kbName == null || kbName.isBlank()) ? "default-kb" : kbName.trim();

        StoredDocument doc = rawTextStore.save(normalizedKb, filename, preprocessResult.getCleanedText());
        log.info("Document ingested: id={}, kb={}", doc.getDocumentId(), doc.getKbName());
        return doc;
    }

    private String extractText(MultipartFile file, String filename) throws IOException {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")
                || lower.endsWith(".json") || lower.endsWith(".log")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        throw new IllegalArgumentException(
                "Cannot extract text from file type: " + filename + ". Supported: .txt, .md, .csv, .json, .log");
    }

    private String safeFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "unknown";
        }
        return Paths.get(original).getFileName().toString();
    }
}
