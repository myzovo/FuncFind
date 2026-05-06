package com.docvecrag.backend.service.preprocess;

import com.docvecrag.backend.service.model.ModelRouterService;
import org.springframework.stereotype.Component;

@Component
public class LightweightTextPreprocessor implements TextPreprocessor {

    private final ModelRouterService modelRouterService;

    public LightweightTextPreprocessor(ModelRouterService modelRouterService) {
        this.modelRouterService = modelRouterService;
    }

    @Override
    public PreprocessResult preprocess(String rawText) {
        return modelRouterService.preprocess(rawText);
    }
}
