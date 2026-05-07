package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.config.Defaults;
import com.docvecrag.backend.model.StoredDocument;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryR2RawTextClient implements R2RawTextClient {

    private final AppProperties appProperties;
    private final Map<String, List<StoredDocument>> r2BucketStore = new ConcurrentHashMap<>();

    public InMemoryR2RawTextClient(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void save(StoredDocument document) {
        assertAvailable();
        r2BucketStore.computeIfAbsent(buildStoreKey(document.getKbName()), ignored -> new ArrayList<>()).add(document);
    }

    @Override
    public List<StoredDocument> findByKbName(String kbName) {
        assertAvailable();
        return new ArrayList<>(r2BucketStore.getOrDefault(buildStoreKey(kbName), List.of()));
    }

    @Override
    public boolean isHealthy() {
        return appProperties.getStorage().getR2().isEnabled()
                && !appProperties.getStorage().getR2().isSimulateFailure();
    }

    private void assertAvailable() {
        if (!appProperties.getStorage().getR2().isEnabled()) {
            throw new IllegalStateException("R2 storage is disabled.");
        }
        if (appProperties.getStorage().getR2().isSimulateFailure()) {
            throw new IllegalStateException("R2 storage simulated failure is enabled.");
        }
    }

    private String buildStoreKey(String kbName) {
        String resolvedKbName = (kbName == null || kbName.isBlank()) ? Defaults.KB_NAME : kbName.trim();
        return resolveBucketName() + "::" + resolvedKbName;
    }

    private String resolveBucketName() {
        SessionRuntimeConfig config = SessionRuntimeConfigContextHolder.getCurrent();
        if (config != null
                && config.getCloudflare() != null
                && config.getCloudflare().getR2Bucket() != null
                && !config.getCloudflare().getR2Bucket().isBlank()) {
            return config.getCloudflare().getR2Bucket().trim();
        }
        return appProperties.getStorage().getR2().getBucket();
    }
}
