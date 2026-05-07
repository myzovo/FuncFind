package com.docvecrag.backend.dto;

import com.docvecrag.backend.config.Defaults;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ChatRequest {
    @NotBlank
    private String question;

    @NotBlank
    private String kbName;

    @Min(1)
    @Max(20)
    private int topK = Defaults.TOP_K;

    private String generationModel = Defaults.GENERATION_MODEL;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getGenerationModel() {
        return generationModel;
    }

    public void setGenerationModel(String generationModel) {
        this.generationModel = generationModel;
    }
}
