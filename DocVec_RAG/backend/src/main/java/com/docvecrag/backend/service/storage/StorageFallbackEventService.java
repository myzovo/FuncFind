package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.model.StoredDocument;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StorageFallbackEventService {

    private final Map<String, StorageFallbackEvent> events = new ConcurrentHashMap<>();

    public StorageFallbackEvent recordFallback(StoredDocument document, String reason, String targetR2Bucket) {
        String eventId = UUID.randomUUID().toString();
        StoredDocument snapshot = new StoredDocument(
                document.getDocumentId(),
                document.getKbName(),
                document.getFilename(),
                document.getRawText(),
                document.getCreatedAt());

        StorageFallbackEvent event = new StorageFallbackEvent(
                eventId,
                snapshot,
                Instant.now(),
                reason == null ? "unknown" : reason,
                targetR2Bucket);

        events.put(eventId, event);
        return event;
    }

    public List<StorageFallbackEvent> listEvents(int limit) {
        return events.values().stream()
                .sorted(Comparator.comparing(StorageFallbackEvent::getOccurredAt).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    public List<StorageFallbackEvent> listPendingEvents(String kbName, int limit) {
        return events.values().stream()
                .filter(event -> event.getStatus() == FallbackStatus.PENDING
                        || event.getStatus() == FallbackStatus.FAILED)
                .filter(event -> kbName == null || kbName.isBlank() || kbName.equals(event.getDocument().getKbName()))
                .sorted(Comparator.comparing(StorageFallbackEvent::getOccurredAt))
                .limit(Math.max(1, limit))
                .toList();
    }

    public void markReplayed(String eventId) {
        StorageFallbackEvent event = events.get(eventId);
        if (event == null) {
            return;
        }
        event.increaseReplayAttempts();
        event.setLastReplayAt(Instant.now());
        event.setLastError(null);
        event.setStatus(FallbackStatus.REPLAYED);
    }

    public void markReplayFailed(String eventId, String error) {
        StorageFallbackEvent event = events.get(eventId);
        if (event == null) {
            return;
        }
        event.increaseReplayAttempts();
        event.setLastReplayAt(Instant.now());
        event.setLastError(error);
        event.setStatus(FallbackStatus.FAILED);
    }

    public int pendingCount() {
        int count = 0;
        for (StorageFallbackEvent event : new ArrayList<>(events.values())) {
            if (event.getStatus() == FallbackStatus.PENDING || event.getStatus() == FallbackStatus.FAILED) {
                count++;
            }
        }
        return count;
    }
}
