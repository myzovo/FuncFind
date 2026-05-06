package com.docvecrag.backend.controller;

import com.docvecrag.backend.dto.CrawlBuildRequest;
import com.docvecrag.backend.dto.KnowledgeBaseBuildRequest;
import com.docvecrag.backend.dto.KnowledgeBaseBuildResponse;
import com.docvecrag.backend.service.CrawlKnowledgeBaseBuildService;
import com.docvecrag.backend.service.KnowledgeBaseBuildService;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import com.docvecrag.backend.service.session.SessionRuntimeConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowledgeBaseController {

    private static final String SESSION_HEADER = "X-Client-Session-Id";

    private final KnowledgeBaseBuildService knowledgeBaseBuildService;
    private final CrawlKnowledgeBaseBuildService crawlKnowledgeBaseBuildService;
    private final SessionRuntimeConfigService sessionRuntimeConfigService;

    public KnowledgeBaseController(KnowledgeBaseBuildService knowledgeBaseBuildService,
            CrawlKnowledgeBaseBuildService crawlKnowledgeBaseBuildService,
            SessionRuntimeConfigService sessionRuntimeConfigService) {
        this.knowledgeBaseBuildService = knowledgeBaseBuildService;
        this.crawlKnowledgeBaseBuildService = crawlKnowledgeBaseBuildService;
        this.sessionRuntimeConfigService = sessionRuntimeConfigService;
    }

    @PostMapping("/build")
    public KnowledgeBaseBuildResponse build(
            @Valid @RequestBody KnowledgeBaseBuildRequest request,
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId) {
        String normalizedKbName = sessionRuntimeConfigService.normalizeKbName(request.getKbName());
        request.setKbName(normalizedKbName);

        SessionRuntimeConfigContextHolder.setCurrent(
                sessionRuntimeConfigService.resolveEffective(sessionId, normalizedKbName));
        try {
            return knowledgeBaseBuildService.build(request);
        } finally {
            SessionRuntimeConfigContextHolder.clear();
        }
    }

    @PostMapping("/build-from-crawl")
    public KnowledgeBaseBuildResponse buildFromCrawl(
            @Valid @RequestBody CrawlBuildRequest request,
            @RequestHeader(name = SESSION_HEADER, required = false) String sessionId) {
        String normalizedKbName = sessionRuntimeConfigService.normalizeKbName(request.getKbName());
        request.setKbName(normalizedKbName);

        SessionRuntimeConfigContextHolder.setCurrent(
                sessionRuntimeConfigService.resolveEffective(sessionId, normalizedKbName));
        try {
            return crawlKnowledgeBaseBuildService.buildFromCrawl(request);
        } finally {
            SessionRuntimeConfigContextHolder.clear();
        }
    }
}
