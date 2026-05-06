package com.docvecrag.backend.service.model;

public class ModelRoutingStatus {
    private String selectedAdapter;
    private String preferredAdapter;
    private String defaultAdapter;
    private String fallbackAdapter;
    private boolean defaultHealthy;
    private boolean fallbackHealthy;

    public String getSelectedAdapter() {
        return selectedAdapter;
    }

    public void setSelectedAdapter(String selectedAdapter) {
        this.selectedAdapter = selectedAdapter;
    }

    public String getPreferredAdapter() {
        return preferredAdapter;
    }

    public void setPreferredAdapter(String preferredAdapter) {
        this.preferredAdapter = preferredAdapter;
    }

    public String getDefaultAdapter() {
        return defaultAdapter;
    }

    public void setDefaultAdapter(String defaultAdapter) {
        this.defaultAdapter = defaultAdapter;
    }

    public String getFallbackAdapter() {
        return fallbackAdapter;
    }

    public void setFallbackAdapter(String fallbackAdapter) {
        this.fallbackAdapter = fallbackAdapter;
    }

    public boolean isDefaultHealthy() {
        return defaultHealthy;
    }

    public void setDefaultHealthy(boolean defaultHealthy) {
        this.defaultHealthy = defaultHealthy;
    }

    public boolean isFallbackHealthy() {
        return fallbackHealthy;
    }

    public void setFallbackHealthy(boolean fallbackHealthy) {
        this.fallbackHealthy = fallbackHealthy;
    }
}
