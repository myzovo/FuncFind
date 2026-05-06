package com.docvecrag.backend.service.storage;

public class StorageReplayResult {
    private int attempted;
    private int replayed;
    private int failed;
    private int pendingAfter;

    public int getAttempted() {
        return attempted;
    }

    public void setAttempted(int attempted) {
        this.attempted = attempted;
    }

    public int getReplayed() {
        return replayed;
    }

    public void setReplayed(int replayed) {
        this.replayed = replayed;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public int getPendingAfter() {
        return pendingAfter;
    }

    public void setPendingAfter(int pendingAfter) {
        this.pendingAfter = pendingAfter;
    }
}
