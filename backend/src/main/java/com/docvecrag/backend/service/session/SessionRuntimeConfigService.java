package com.docvecrag.backend.service.session;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.dto.RuntimeConfigApplyRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionRuntimeConfigService {

    public static final String DEFAULT_KB_NAME = "default-kb";
    private static final String DEFAULT_GENERATION_MODEL = "gpt-4o-mini";

    private final AppProperties appProperties;
    private final Map<String, SessionRuntimeConfig> configs = new ConcurrentHashMap<>();

    public SessionRuntimeConfigService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public SessionRuntimeConfig apply(String sessionId, RuntimeConfigApplyRequest request) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        String normalizedKbName = normalizeKbName(request.getKbName());
        String key = buildKey(normalizedSessionId, normalizedKbName);

        SessionRuntimeConfig existing = configs.get(key);
        SessionRuntimeConfig baseline = existing == null
                ? buildDefaultConfig(normalizedSessionId, normalizedKbName)
                : cloneConfig(existing);

        RuntimeConfigApplyRequest.Cloudflare cloudflareRequest = request.getCloudflare();
        if (cloudflareRequest != null) {
            baseline.getCloudflare().setAccountId(resolveValue(
                    cloudflareRequest.getAccountId(),
                    baseline.getCloudflare().getAccountId()));
            baseline.getCloudflare().setApiToken(resolveSecret(
                    cloudflareRequest.getApiToken(),
                    baseline.getCloudflare().getApiToken()));
            baseline.getCloudflare().setVectorizeIndexName(resolveValue(
                    cloudflareRequest.getVectorizeIndexName(),
                    baseline.getCloudflare().getVectorizeIndexName()));
            baseline.getCloudflare().setVectorizeNamespace(resolveValue(
                    cloudflareRequest.getVectorizeNamespace(),
                    baseline.getCloudflare().getVectorizeNamespace()));
            baseline.getCloudflare().setR2Bucket(resolveValue(
                    cloudflareRequest.getR2Bucket(),
                    baseline.getCloudflare().getR2Bucket()));
        }

        RuntimeConfigApplyRequest.Generation generationRequest = request.getGeneration();
        if (generationRequest != null) {
            baseline.getGeneration().setProviderType(resolveProviderType(generationRequest.getProviderType(),
                    baseline.getGeneration().getProviderType()));
            baseline.getGeneration().setBaseUrl(resolveValue(
                    generationRequest.getBaseUrl(),
                    baseline.getGeneration().getBaseUrl()));
            baseline.getGeneration().setApiKey(resolveSecret(
                    generationRequest.getApiKey(),
                    baseline.getGeneration().getApiKey()));
            baseline.getGeneration().setGenerationModelId(resolveValue(
                    generationRequest.getGenerationModelId(),
                    baseline.getGeneration().getGenerationModelId()));
        }

        baseline.setSessionId(normalizedSessionId);
        baseline.setKbName(normalizedKbName);
        baseline.setAppliedAt(Instant.now());
        configs.put(key, baseline);
        return cloneConfig(baseline);
    }

    public SessionRuntimeConfig current(String sessionId, String kbName) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        String normalizedKbName = normalizeKbName(kbName);
        SessionRuntimeConfig existing = configs.get(buildKey(normalizedSessionId, normalizedKbName));
        if (existing == null) {
            return null;
        }
        return cloneConfig(existing);
    }

    public SessionRuntimeConfig resolveEffective(String sessionId, String kbName) {
        String normalizedKbName = normalizeKbName(kbName);
        if (sessionId == null || sessionId.isBlank()) {
            return buildDefaultConfig("anonymous", normalizedKbName);
        }

        SessionRuntimeConfig existing = configs.get(buildKey(sessionId.trim(), normalizedKbName));
        if (existing == null) {
            return buildDefaultConfig(sessionId.trim(), normalizedKbName);
        }
        return cloneConfig(existing);
    }

    public SessionRuntimeConfig resolveCurrentOrDefault(String sessionId, String kbName) {
        SessionRuntimeConfig currentConfig = current(sessionId, kbName);
        if (currentConfig != null) {
            return currentConfig;
        }
        return resolveEffective(sessionId, kbName);
    }

    public String normalizeKbName(String kbName) {
        if (kbName == null || kbName.isBlank()) {
            return DEFAULT_KB_NAME;
        }
        return kbName.trim();
    }

    private SessionRuntimeConfig buildDefaultConfig(String sessionId, String kbName) {
        SessionRuntimeConfig config = new SessionRuntimeConfig();
        config.setSessionId(sessionId);
        config.setKbName(kbName);
        config.setAppliedAt(null);

        config.getCloudflare().setAccountId("");
        config.getCloudflare().setApiToken("");
        config.getCloudflare().setVectorizeIndexName(appProperties.getVectorize().getIndexName());
        config.getCloudflare().setVectorizeNamespace(appProperties.getVectorize().getNamespace());
        config.getCloudflare().setR2Bucket(appProperties.getStorage().getR2().getBucket());

        config.getGeneration()
                .setProviderType(resolveProviderType(null, appProperties.getModelRouting().getDefaultAdapter()));
        config.getGeneration().setBaseUrl(appProperties.getExternalModelService().getBaseUrl());
        config.getGeneration().setApiKey("");
        config.getGeneration().setGenerationModelId(DEFAULT_GENERATION_MODEL);

        return config;
    }

    private SessionRuntimeConfig cloneConfig(SessionRuntimeConfig source) {
        SessionRuntimeConfig clone = new SessionRuntimeConfig();
        clone.setSessionId(source.getSessionId());
        clone.setKbName(source.getKbName());
        clone.setAppliedAt(source.getAppliedAt());

        clone.getCloudflare().setAccountId(source.getCloudflare().getAccountId());
        clone.getCloudflare().setApiToken(source.getCloudflare().getApiToken());
        clone.getCloudflare().setVectorizeIndexName(source.getCloudflare().getVectorizeIndexName());
        clone.getCloudflare().setVectorizeNamespace(source.getCloudflare().getVectorizeNamespace());
        clone.getCloudflare().setR2Bucket(source.getCloudflare().getR2Bucket());

        clone.getGeneration().setProviderType(source.getGeneration().getProviderType());
        clone.getGeneration().setBaseUrl(source.getGeneration().getBaseUrl());
        clone.getGeneration().setApiKey(source.getGeneration().getApiKey());
        clone.getGeneration().setGenerationModelId(source.getGeneration().getGenerationModelId());
        return clone;
    }

    private String buildKey(String sessionId, String kbName) {
        return sessionId + "::" + kbName;
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "anonymous";
        }
        return sessionId.trim();
    }

    private String resolveValue(String incoming, String existingOrDefault) {
        if (incoming == null) {
            return existingOrDefault;
        }

        String trimmed = incoming.trim();
        if (trimmed.isEmpty()) {
            return existingOrDefault;
        }
        return trimmed;
    }

    private String resolveSecret(String incoming, String existingOrDefault) {
        if (incoming == null) {
            return existingOrDefault;
        }

        String trimmed = incoming.trim();
        if (trimmed.isEmpty()) {
            return existingOrDefault;
        }
        return trimmed;
    }

    private String resolveProviderType(String incoming, String fallback) {
        String resolved = resolveValue(incoming, fallback);
        if (resolved == null || resolved.isBlank()) {
            return "workers-ai";
        }

        String normalized = resolved.trim().toLowerCase();
        if ("workers-ai".equals(normalized) || "external".equals(normalized)) {
            return normalized;
        }
        return "workers-ai";
    }
}
