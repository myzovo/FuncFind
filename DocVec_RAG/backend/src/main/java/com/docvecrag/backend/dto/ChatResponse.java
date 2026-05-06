package com.docvecrag.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {
    private String answer;
    private String kbName;
    private String generationModel;
    private int retrievedCount;
    private List<ContextChunkResponse> contextChunks = new ArrayList<>();
    private List<LocationHintResponse> locations = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }

    public int getRetrievedCount() {
        return retrievedCount;
    }

    public void setRetrievedCount(int retrievedCount) {
        this.retrievedCount = retrievedCount;
    }

    public List<ContextChunkResponse> getContextChunks() {
        return contextChunks;
    }

    public void setContextChunks(List<ContextChunkResponse> contextChunks) {
        this.contextChunks = contextChunks;
    }

    public List<LocationHintResponse> getLocations() {
        return locations;
    }

    public void setLocations(List<LocationHintResponse> locations) {
        this.locations = locations;
    }
}
