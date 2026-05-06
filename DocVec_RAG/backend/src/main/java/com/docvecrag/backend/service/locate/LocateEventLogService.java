package com.docvecrag.backend.service.locate;

import com.docvecrag.backend.dto.LocateReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Service
public class LocateEventLogService {

    private final ObjectMapper objectMapper;
    private final Path logPath = Paths.get("logs", "locate-events.jsonl");
    private final Object writeLock = new Object();

    public LocateEventLogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void append(LocateReportRequest request) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("reportedAt", Instant.now().toString());
        node.put("kbName", request.getKbName());
        node.put("status", request.getStatus());
        putIfPresent(node, "pageUrl", request.getPageUrl());
        putIfPresent(node, "selector", request.getSelector());
        putIfPresent(node, "xpath", request.getXpath());
        putIfPresent(node, "elementText", request.getElementText());
        putIfPresent(node, "reason", request.getReason());
        putIfPresent(node, "method", request.getMethod());
        putIfPresent(node, "sessionId", request.getSessionId());
        putIfPresent(node, "details", request.getDetails());

        try {
            Files.createDirectories(logPath.getParent());
            String payload = objectMapper.writeValueAsString(node) + System.lineSeparator();
            synchronized (writeLock) {
                Files.writeString(logPath, payload, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write locate event log.", ex);
        }
    }

    private void putIfPresent(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) {
            node.put(key, value.trim());
        }
    }
}
