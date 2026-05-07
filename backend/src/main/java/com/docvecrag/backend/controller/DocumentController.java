package com.docvecrag.backend.controller;

import com.docvecrag.backend.dto.DocumentUploadResponse;
import com.docvecrag.backend.model.StoredDocument;
import com.docvecrag.backend.service.DocumentIngestionService;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import com.docvecrag.backend.service.session.SessionRuntimeConfigService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final String SESSION_HEADER = "X-Client-Session-Id";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".md", ".csv", ".json", ".log");

    private final DocumentIngestionService documentIngestionService;
    private final SessionRuntimeConfigService sessionRuntimeConfigService;

    public DocumentController(DocumentIngestionService documentIngestionService,
            SessionRuntimeConfigService sessionRuntimeConfigService) {
        this.documentIngestionService = documentIngestionService;
        this.sessionRuntimeConfigService = sessionRuntimeConfigService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "kbName", required = false) String kbName,
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId) throws IOException {
        validateFile(file);

        String normalizedKbName = sessionRuntimeConfigService.normalizeKbName(kbName);
        SessionRuntimeConfigContextHolder.setCurrent(
                sessionRuntimeConfigService.resolveEffective(sessionId, normalizedKbName));

        StoredDocument stored;
        try {
            stored = documentIngestionService.ingest(file, normalizedKbName);
        } finally {
            SessionRuntimeConfigContextHolder.clear();
        }

        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(stored.getDocumentId());
        response.setKbName(stored.getKbName());
        response.setFilename(stored.getFilename());
        response.setCreatedAt(stored.getCreatedAt() == null ? null : stored.getCreatedAt().toString());
        response.setSize(file.getSize());
        response.setStatus("UPLOADED");
        return response;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is missing.");
        }
        String lower = filename.toLowerCase();
        boolean allowed = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!allowed) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }
}
