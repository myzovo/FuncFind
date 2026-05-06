package com.docvecrag.backend.service.embedding;

import java.util.ArrayList;
import java.util.List;

public final class EmbeddingVectorUtil {

    private EmbeddingVectorUtil() {
    }

    public static List<Float> deterministicVector(String text, int dimension) {
        List<Float> vector = new ArrayList<>(dimension);
        int seed = (text == null ? 17 : text.hashCode()) ^ 0x9E3779B9;

        for (int i = 0; i < dimension; i++) {
            seed = seed * 1664525 + 1013904223;
            float value = ((seed >>> 8) % 2000) / 1000.0f - 1.0f;
            vector.add(value);
        }
        return normalize(vector);
    }

    private static List<Float> normalize(List<Float> vector) {
        double sum = 0.0;
        for (Float f : vector) {
            sum += f * f;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return vector;
        }

        List<Float> normalized = new ArrayList<>(vector.size());
        for (Float f : vector) {
            normalized.add((float) (f / norm));
        }
        return normalized;
    }
}
