package com.docvecrag.backend.service.model;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.model.RetrievedChunk;
import com.docvecrag.backend.service.embedding.EmbeddingVectorUtil;
import com.docvecrag.backend.service.preprocess.PreprocessResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkersAiModelAdapter implements ModelAdapter {

    private final AppProperties appProperties;

    public WorkersAiModelAdapter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String adapterId() {
        return "workers-ai";
    }

    @Override
    public boolean isHealthy() {
        return appProperties.getWorkersAi().isEnabled() && !appProperties.getWorkersAi().isSimulateFailure();
    }

    @Override
    public PreprocessResult preprocess(String rawText) {
        String normalized = rawText == null ? ""
                : rawText
                        .replace("\r\n", "\n")
                        .replaceAll("[\t ]+", " ")
                        .replaceAll("\n{3,}", "\n\n")
                        .trim();

        String title = "untitled";
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                title = trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
                break;
            }
        }

        return new PreprocessResult(title, normalized);
    }

    @Override
    public List<Float> embed(String text, int dimension) {
        return EmbeddingVectorUtil.deterministicVector(text, dimension);
    }

    @Override
    public String generate(String question, List<RetrievedChunk> contexts, String generationModel) {
        String model = (generationModel == null || generationModel.isBlank()) ? "gpt-4o-mini" : generationModel;

        StringBuilder builder = new StringBuilder();
        builder.append("[adapter: workers-ai] [model: ").append(model).append("]\n");
        builder.append("问题: ").append(question).append("\n\n");

        if (contexts == null || contexts.isEmpty()) {
            builder.append("未检索到上下文，请先上传文档并构建知识库。");
            return builder.toString();
        }

        builder.append("基于检索上下文生成回答（Workers AI 路由）:\n");
        int limit = Math.min(3, contexts.size());
        for (int i = 0; i < limit; i++) {
            RetrievedChunk chunk = contexts.get(i);
            String preview = chunk.getText();
            if (preview.length() > 120) {
                preview = preview.substring(0, 120) + "...";
            }
            builder.append(i + 1)
                    .append(". [")
                    .append(chunk.getSourceDocId())
                    .append("] ")
                    .append(preview)
                    .append("\n");
        }
        return builder.toString().trim();
    }
}
