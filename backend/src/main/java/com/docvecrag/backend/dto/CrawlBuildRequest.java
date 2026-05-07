package com.docvecrag.backend.dto;

import com.docvecrag.backend.config.Defaults;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

public class CrawlBuildRequest {
    @NotBlank
    private String kbName;

    private String siteId;

    private String embeddingModel = Defaults.EMBEDDING_MODEL;

    @NotEmpty
    private List<CrawlPageRequest> pages = new ArrayList<>();

    public String getKbName() {
        return kbName;
    }

    public void setKbName(String kbName) {
        this.kbName = kbName;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<CrawlPageRequest> getPages() {
        return pages;
    }

    public void setPages(List<CrawlPageRequest> pages) {
        this.pages = pages;
    }

    public static class CrawlPageRequest {
        private String url;
        private String title;
        private List<CrawlElementRequest> elements = new ArrayList<>();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<CrawlElementRequest> getElements() {
            return elements;
        }

        public void setElements(List<CrawlElementRequest> elements) {
            this.elements = elements;
        }
    }

    public static class CrawlElementRequest {
        private String type;
        private String text;
        private String href;
        private String selector;
        private String context;
        private String contextText;
        private String nearestHeading;
        private String tag;
        private String role;
        private String xpath;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getSelector() {
            return selector;
        }

        public void setSelector(String selector) {
            this.selector = selector;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getContextText() {
            return contextText;
        }

        public void setContextText(String contextText) {
            this.contextText = contextText;
        }

        public String getNearestHeading() {
            return nearestHeading;
        }

        public void setNearestHeading(String nearestHeading) {
            this.nearestHeading = nearestHeading;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getXpath() {
            return xpath;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }
    }
}
