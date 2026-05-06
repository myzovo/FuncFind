package com.docvecrag.backend.service.preprocess;

public class PreprocessResult {
    private String title;
    private String cleanedText;

    public PreprocessResult(String title, String cleanedText) {
        this.title = title;
        this.cleanedText = cleanedText;
    }

    public String getTitle() {
        return title;
    }

    public String getCleanedText() {
        return cleanedText;
    }
}
