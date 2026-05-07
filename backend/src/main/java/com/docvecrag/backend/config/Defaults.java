package com.docvecrag.backend.config;

/**
 * Central place for default values used across the application.
 * Eliminates duplication of magic strings/numbers in DTOs, Services, and Adapters.
 */
public final class Defaults {

    private Defaults() {}

    public static final String GENERATION_MODEL = "gpt-4o-mini";
    public static final String EMBEDDING_MODEL = "all-MiniLM-L6-v2";
    public static final String KB_NAME = "default-kb";
    public static final int TOP_K = 6;
    public static final int CHUNK_SIZE = 500;
    public static final double SEMANTIC_THRESHOLD = 0.78;
    public static final String EXTERNAL_MODEL_BASE_URL = "http://localhost:9000";
}
