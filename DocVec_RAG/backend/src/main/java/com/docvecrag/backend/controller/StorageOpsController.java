package com.docvecrag.backend.controller;

import com.docvecrag.backend.dto.StorageFallbackEventResponse;
import com.docvecrag.backend.dto.StorageReplayRequest;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import com.docvecrag.backend.service.session.SessionRuntimeConfigService;
import com.docvecrag.backend.service.storage.StorageFallbackEvent;
import com.docvecrag.backend.service.storage.StorageFallbackEventService;
import com.docvecrag.backend.service.storage.StorageReplayResult;
import com.docvecrag.backend.service.storage.StorageReplayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/storage")
public class StorageOpsController {

    private static final String SESSION_HEADER = "X-Client-Session-Id";

    private final StorageFallbackEventService fallbackEventService;
    private final StorageReplayService storageReplayService;
    private final SessionRuntimeConfigService sessionRuntimeConfigService;

    public StorageOpsController(StorageFallbackEventService fallbackEventService,
            StorageReplayService storageReplayService,
            SessionRuntimeConfigService sessionRuntimeConfigService) {
        this.fallbackEventService = fallbackEventService;
        this.storageReplayService = storageReplayService;
        this.sessionRuntimeConfigService = sessionRuntimeConfigService;
    }

    @GetMapping("/fallback/events")
    public List<StorageFallbackEventResponse> listFallbackEvents(
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
        return fallbackEventService.listEvents(limit).stream().map(this::toResponse).toList();
    }

    @PostMapping("/fallback/replay")
    public StorageReplayResult replayFallback(
            @RequestBody(required = false) StorageReplayRequest request,
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId) {
        String kbName = request == null ? null : request.getKbName();
        Integer limit = request == null ? null : request.getLimit();
        String normalizedKbName = (kbName == null || kbName.isBlank())
                ? null
                : sessionRuntimeConfigService.normalizeKbName(kbName);

        SessionRuntimeConfigContextHolder.setCurrent(
                sessionRuntimeConfigService.resolveEffective(
                        sessionId,
                        normalizedKbName == null ? SessionRuntimeConfigService.DEFAULT_KB_NAME : normalizedKbName));
        try {
            return storageReplayService.replayPending(normalizedKbName, limit);
        } finally {
            SessionRuntimeConfigContextHolder.clear();
        }
    }

    private StorageFallbackEventResponse toResponse(StorageFallbackEvent event) {
        StorageFallbackEventResponse response = new StorageFallbackEventResponse();
        response.setEventId(event.getEventId());
        response.setStatus(event.getStatus().name());
        response.setDocumentId(event.getDocument().getDocumentId());
        response.setKbName(event.getDocument().getKbName());
        response.setFilename(event.getDocument().getFilename());
        response.setFallbackReason(event.getFallbackReason());
        response.setTargetR2Bucket(event.getTargetR2Bucket());
        response.setReplayAttempts(event.getReplayAttempts());
        response.setOccurredAt(event.getOccurredAt() == null ? null : event.getOccurredAt().toString());
        response.setLastReplayAt(event.getLastReplayAt() == null ? null : event.getLastReplayAt().toString());
        response.setLastError(event.getLastError());
        return response;
    }
}
