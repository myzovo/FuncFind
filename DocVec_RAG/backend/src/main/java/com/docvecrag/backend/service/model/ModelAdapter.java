package com.docvecrag.backend.service.model;

import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.preprocess.PreprocessResult;

import java.util.List;

public interface ModelAdapter {
    String adapterId();

    boolean isHealthy();

    PreprocessResult preprocess(String rawText);

    List<Float> embed(String text, int dimension);

    String generate(String question, List<RetrievedChunk> contexts, String generationModel);
}
