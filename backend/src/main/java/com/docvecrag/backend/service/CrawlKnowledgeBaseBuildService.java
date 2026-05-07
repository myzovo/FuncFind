package com.docvecrag.backend.service;

import com.docvecrag.backend.dto.CrawlBuildRequest;
import com.docvecrag.backend.dto.KnowledgeBaseBuildResponse;
import com.docvecrag.backend.model.IndexedChunk;
import com.docvecrag.backend.service.embedding.EmbeddingBinding;
import com.docvecrag.backend.service.embedding.EmbeddingService;
import com.docvecrag.backend.service.vector.VectorStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CrawlKnowledgeBaseBuildService {

    private static final Logger log = LoggerFactory.getLogger(CrawlKnowledgeBaseBuildService.class);

    private final EmbeddingService embeddingService;
    private final VectorStoreClient vectorStoreClient;

    public CrawlKnowledgeBaseBuildService(EmbeddingService embeddingService, VectorStoreClient vectorStoreClient) {
        this.embeddingService = embeddingService;
        this.vectorStoreClient = vectorStoreClient;
    }

    public KnowledgeBaseBuildResponse buildFromCrawl(CrawlBuildRequest request) {
        List<CrawlBuildRequest.CrawlPageRequest> pages = request.getPages();
        if (pages == null || pages.isEmpty()) {
            throw new IllegalArgumentException("No pages found in crawl payload.");
        }
        log.info("Building from crawl: kb={}, pages={}, model={}", request.getKbName(), pages.size(), request.getEmbeddingModel());

        EmbeddingBinding binding = embeddingService.bindForIndexing(request.getEmbeddingModel());

        Instant now = Instant.now();
        List<IndexedChunk> indexed = new ArrayList<>();

        for (CrawlBuildRequest.CrawlPageRequest page : pages) {
            String pageUrl = safeTrim(page.getUrl());
            String pageTitle = safeTrim(page.getTitle());
            List<CrawlBuildRequest.CrawlElementRequest> elements = page.getElements();
            if (elements == null || elements.isEmpty()) {
                continue;
            }

            for (CrawlBuildRequest.CrawlElementRequest element : elements) {
                String elementText = safeTrim(element.getText());
                String selector = safeTrim(element.getSelector());
                String href = safeTrim(element.getHref());
                String context = safeTrim(element.getContext());
                String contextText = safeTrim(element.getContextText());
                String nearestHeading = safeTrim(element.getNearestHeading());
                String tag = safeTrim(element.getTag());
                String role = safeTrim(element.getRole());
                String xpath = safeTrim(element.getXpath());
                String type = safeTrim(element.getType());

                if (elementText.isEmpty() && selector.isEmpty() && href.isEmpty()) {
                    continue;
                }

                String chunkText = buildChunkText(pageTitle, pageUrl, elementText, context, contextText,
                        nearestHeading, type, href);
                Map<String, String> metadata = buildMetadata(pageUrl, pageTitle, elementText, type, selector, href,
                        context, contextText, nearestHeading, tag, role, xpath, chunkText);

                IndexedChunk chunk = new IndexedChunk(
                        UUID.randomUUID().toString(),
                        buildSourceDocId(pageUrl, pageTitle),
                        now,
                        chunkText,
                        embeddingService.embedForIndexing(chunkText, request.getEmbeddingModel()),
                        metadata);
                indexed.add(chunk);
            }
        }

        if (indexed.isEmpty()) {
            throw new IllegalStateException("No actionable elements found in crawl payload.");
        }

        vectorStoreClient.upsert(request.getKbName(), indexed);
        log.info("Indexed {} crawl chunks for kb={}", indexed.size(), request.getKbName());

        KnowledgeBaseBuildResponse response = new KnowledgeBaseBuildResponse();
        response.setKbName(request.getKbName());
        response.setDocumentCount(pages.size());
        response.setIndexedChunkCount(indexed.size());
        response.setEmbeddingModel(binding.getModelId());
        response.setEmbeddingDimension(binding.getDimension());
        response.setStatus("SUCCESS");
        response.setMessage("Crawl payload indexed into vector store.");
        return response;
    }

    private String buildChunkText(String pageTitle, String pageUrl, String elementText,
            String context, String contextText, String nearestHeading, String type, String href) {
        StringBuilder sb = new StringBuilder();
        if (!pageTitle.isEmpty()) {
            sb.append("Page: ").append(pageTitle).append("\n");
        }
        if (!pageUrl.isEmpty()) {
            sb.append("URL: ").append(pageUrl).append("\n");
        }
        if (!nearestHeading.isEmpty()) {
            sb.append("Heading: ").append(nearestHeading).append("\n");
        }
        if (!context.isEmpty()) {
            sb.append("Context: ").append(context).append("\n");
        }
        if (!contextText.isEmpty()) {
            sb.append("ContextText: ").append(contextText).append("\n");
        }
        if (!elementText.isEmpty()) {
            sb.append("Element: ").append(elementText).append("\n");
        }
        if (!type.isEmpty()) {
            sb.append("Type: ").append(type).append("\n");
        }
        if (!href.isEmpty()) {
            sb.append("Href: ").append(href);
        }
        return sb.toString().trim();
    }

    private Map<String, String> buildMetadata(String pageUrl, String pageTitle, String elementText,
            String elementType, String selector, String href, String context, String contextText,
            String nearestHeading, String tag, String role, String xpath, String evidence) {
        Map<String, String> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "pageUrl", pageUrl);
        putIfPresent(metadata, "pageTitle", pageTitle);
        putIfPresent(metadata, "elementText", elementText);
        putIfPresent(metadata, "elementType", elementType);
        putIfPresent(metadata, "selector", selector);
        putIfPresent(metadata, "href", href);
        putIfPresent(metadata, "context", context);
        putIfPresent(metadata, "contextText", contextText);
        putIfPresent(metadata, "nearestHeading", nearestHeading);
        putIfPresent(metadata, "tag", tag);
        putIfPresent(metadata, "role", role);
        putIfPresent(metadata, "xpath", xpath);
        putIfPresent(metadata, "evidence", evidence);
        metadata.putIfAbsent("sourceType", "crawl");
        return metadata;
    }

    private void putIfPresent(Map<String, String> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value.trim());
        }
    }

    private String buildSourceDocId(String pageUrl, String pageTitle) {
        if (pageUrl != null && !pageUrl.isBlank()) {
            return pageUrl.trim();
        }
        if (pageTitle != null && !pageTitle.isBlank()) {
            return "page:" + pageTitle.trim();
        }
        return "page:" + UUID.randomUUID();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
