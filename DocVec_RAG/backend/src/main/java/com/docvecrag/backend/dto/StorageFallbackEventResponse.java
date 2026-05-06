package com.docvecrag.backend.dto;

public class StorageFallbackEventResponse {
    private String eventId;
    private String status;
    private String documentId;
    private String kbName;
    private String filename;
    private String fallbackReason;
    private String targetR2Bucket;
    private int replayAttempts;
    private String occurredAt;
    private String lastReplayAt;
    private String lastError;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public String getTargetR2Bucket() {
        return targetR2Bucket;
    }

    public void setTargetR2Bucket(String targetR2Bucket) {
        this.targetR2Bucket = targetR2Bucket;
    }

    public int getReplayAttempts() {
        return replayAttempts;
    }

    public void setReplayAttempts(int replayAttempts) {
        this.replayAttempts = replayAttempts;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getLastReplayAt() {
        return lastReplayAt;
    }

    public void setLastReplayAt(String lastReplayAt) {
        this.lastReplayAt = lastReplayAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
