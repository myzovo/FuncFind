package com.docvecrag.backend.controller;

import com.docvecrag.backend.service.model.ModelRouterService;
import com.docvecrag.backend.service.model.ModelRoutingStatus;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import com.docvecrag.backend.service.session.SessionRuntimeConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelOpsController {

    private static final String SESSION_HEADER = "X-Client-Session-Id";

    private final ModelRouterService modelRouterService;
    private final SessionRuntimeConfigService sessionRuntimeConfigService;

    public ModelOpsController(ModelRouterService modelRouterService,
            SessionRuntimeConfigService sessionRuntimeConfigService) {
        this.modelRouterService = modelRouterService;
        this.sessionRuntimeConfigService = sessionRuntimeConfigService;
    }

    @GetMapping("/routing/status")
    public ModelRoutingStatus routingStatus(
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId,
            @RequestParam(name = "kbName", required = false) String kbName) {
        String normalizedKbName = sessionRuntimeConfigService.normalizeKbName(kbName);
        SessionRuntimeConfigContextHolder.setCurrent(
                sessionRuntimeConfigService.resolveEffective(sessionId, normalizedKbName));
        try {
            return modelRouterService.routingStatus();
        } finally {
            SessionRuntimeConfigContextHolder.clear();
        }
    }
}
