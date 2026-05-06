package com.docvecrag.backend.service;

import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.model.ModelRouterService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerGenerationService {

    private final ModelRouterService modelRouterService;

    public AnswerGenerationService(ModelRouterService modelRouterService) {
        this.modelRouterService = modelRouterService;
    }

    public String generate(String question, List<RetrievedChunk> contexts, String generationModel) {
        return modelRouterService.generate(question, contexts, generationModel);
    }
}
