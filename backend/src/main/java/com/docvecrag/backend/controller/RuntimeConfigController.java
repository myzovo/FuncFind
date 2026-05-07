package com.docvecrag.backend.controller;

import com.docvecrag.backend.dto.RuntimeConfigApplyRequest;
import com.docvecrag.backend.dto.RuntimeConfigResponse;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime-config")
public class RuntimeConfigController {

    public static final String SESSION_HEADER = "X-Client-Session-Id";

    private final SessionRuntimeConfigService sessionRuntimeConfigService;

    public RuntimeConfigController(SessionRuntimeConfigService sessionRuntimeConfigService) {
        this.sessionRuntimeConfigService = sessionRuntimeConfigService;
    }

    @PostMapping("/apply")
    public RuntimeConfigResponse apply(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId,
            @Valid @RequestBody RuntimeConfigApplyRequest request) {
        SessionRuntimeConfig config = sessionRuntimeConfigService.apply(sessionId, request);
        return toResponse(config);
    }

    @GetMapping("/current")
    public RuntimeConfigResponse current(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId,
            @RequestParam(name = "kbName", required = false) String kbName) {
        SessionRuntimeConfig config = sessionRuntimeConfigService.resolveCurrentOrDefault(sessionId, kbName);
        return toResponse(config);
    }

    private RuntimeConfigResponse toResponse(SessionRuntimeConfig config) {
        RuntimeConfigResponse response = new RuntimeConfigResponse();
        response.setSessionId(config.getSessionId());
        response.setKbName(config.getKbName());
        response.setAppliedAt(config.getAppliedAt() == null ? null : config.getAppliedAt().toString());

        RuntimeConfigResponse.Cloudflare cloudflare = new RuntimeConfigResponse.Cloudflare();
        cloudflare.setAccountId(config.getCloudflare().getAccountId());
        cloudflare.setVectorizeIndexName(config.getCloudflare().getVectorizeIndexName());
        cloudflare.setVectorizeNamespace(config.getCloudflare().getVectorizeNamespace());
        cloudflare.setR2Bucket(config.getCloudflare().getR2Bucket());
        cloudflare.setHasApiToken(hasText(config.getCloudflare().getApiToken()));
        cloudflare.setApiTokenMasked(maskSecret(config.getCloudflare().getApiToken()));
        response.setCloudflare(cloudflare);

        RuntimeConfigResponse.Generation generation = new RuntimeConfigResponse.Generation();
        generation.setProviderType(config.getGeneration().getProviderType());
        generation.setBaseUrl(config.getGeneration().getBaseUrl());
        generation.setGenerationModelId(config.getGeneration().getGenerationModelId());
        generation.setHasApiKey(hasText(config.getGeneration().getApiKey()));
        generation.setApiKeyMasked(maskSecret(config.getGeneration().getApiKey()));
        response.setGeneration(generation);

        return response;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String maskSecret(String secret) {
        if (!hasText(secret)) {
            return "";
        }

        String trimmed = secret.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return trimmed.substring(0, 2) + "***" + trimmed.substring(trimmed.length() - 2);
    }
}
