package com.docvecrag.backend.dto;

public class StorageReplayRequest {
    private String kbName;
    private Integer limit;

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
