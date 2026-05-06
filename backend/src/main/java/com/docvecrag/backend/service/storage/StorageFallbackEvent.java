package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.model.StoredDocument;

import java.time.Instant;

public class StorageFallbackEvent {
    private final String eventId;
    private final StoredDocument document;
    private final Instant occurredAt;
    private final String fallbackReason;
    private final String targetR2Bucket;

    private FallbackStatus status;
    private int replayAttempts;
    private Instant lastReplayAt;
    private String lastError;

    public StorageFallbackEvent(String eventId,
            StoredDocument document,
            Instant occurredAt,
            String fallbackReason,
            String targetR2Bucket) {
        this.eventId = eventId;
        this.document = document;
        this.occurredAt = occurredAt;
        this.fallbackReason = fallbackReason;
        this.targetR2Bucket = targetR2Bucket;
        this.status = FallbackStatus.PENDING;
    }

    public String getEventId() {
        return eventId;
    }

    public StoredDocument getDocument() {
        return document;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public String getTargetR2Bucket() {
        return targetR2Bucket;
    }

    public FallbackStatus getStatus() {
        return status;
    }

    public void setStatus(FallbackStatus status) {
        this.status = status;
    }

    public int getReplayAttempts() {
        return replayAttempts;
    }

    public void increaseReplayAttempts() {
        this.replayAttempts += 1;
    }

    public Instant getLastReplayAt() {
        return lastReplayAt;
    }

    public void setLastReplayAt(Instant lastReplayAt) {
        this.lastReplayAt = lastReplayAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
