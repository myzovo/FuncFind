package com.docvecrag.backend.service.storage;

import com.docvecrag.backend.config.AppProperties;
import com.docvecrag.backend.service.session.SessionRuntimeConfig;
import com.docvecrag.backend.service.session.SessionRuntimeConfigContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StorageReplayService {

    private final AppProperties appProperties;
    private final R2RawTextClient r2RawTextClient;
    private final StorageFallbackEventService fallbackEventService;

    public StorageReplayService(AppProperties appProperties,
            R2RawTextClient r2RawTextClient,
            StorageFallbackEventService fallbackEventService) {
        this.appProperties = appProperties;
        this.r2RawTextClient = r2RawTextClient;
        this.fallbackEventService = fallbackEventService;
    }

    public StorageReplayResult replayPending(String kbName, Integer limit) {
        int replayLimit = (limit == null || limit <= 0)
                ? appProperties.getStorage().getReplay().getBatchSize()
                : limit;

        List<StorageFallbackEvent> pending = fallbackEventService.listPendingEvents(kbName, replayLimit);

        StorageReplayResult result = new StorageReplayResult();
        result.setAttempted(pending.size());

        int replayed = 0;
        int failed = 0;
        SessionRuntimeConfig baselineContext = SessionRuntimeConfigContextHolder.getCurrent();

        for (StorageFallbackEvent event : pending) {
            try {
                SessionRuntimeConfigContextHolder.setCurrent(buildReplayContext(baselineContext, event));
                r2RawTextClient.save(event.getDocument());
                fallbackEventService.markReplayed(event.getEventId());
                replayed++;
            } catch (Exception ex) {
                fallbackEventService.markReplayFailed(event.getEventId(), ex.getMessage());
                failed++;
            } finally {
                if (baselineContext != null) {
                    SessionRuntimeConfigContextHolder.setCurrent(baselineContext);
                } else {
                    SessionRuntimeConfigContextHolder.clear();
                }
            }
        }

        result.setReplayed(replayed);
        result.setFailed(failed);
        result.setPendingAfter(fallbackEventService.pendingCount());
        return result;
    }

    private SessionRuntimeConfig buildReplayContext(SessionRuntimeConfig baselineContext, StorageFallbackEvent event) {
        SessionRuntimeConfig context = new SessionRuntimeConfig();
        context.setSessionId(baselineContext == null ? "replay" : baselineContext.getSessionId());
        context.setKbName(event.getDocument().getKbName());
        context.setAppliedAt(null);

        if (baselineContext != null) {
            context.getCloudflare().setAccountId(baselineContext.getCloudflare().getAccountId());
            context.getCloudflare().setApiToken(baselineContext.getCloudflare().getApiToken());
            context.getCloudflare().setVectorizeIndexName(baselineContext.getCloudflare().getVectorizeIndexName());
            context.getCloudflare().setVectorizeNamespace(baselineContext.getCloudflare().getVectorizeNamespace());
            context.getGeneration().setProviderType(baselineContext.getGeneration().getProviderType());
            context.getGeneration().setBaseUrl(baselineContext.getGeneration().getBaseUrl());
            context.getGeneration().setApiKey(baselineContext.getGeneration().getApiKey());
            context.getGeneration().setGenerationModelId(baselineContext.getGeneration().getGenerationModelId());
        }

        String targetBucket = event.getTargetR2Bucket();
        if (targetBucket != null && !targetBucket.isBlank()) {
            context.getCloudflare().setR2Bucket(targetBucket.trim());
        } else if (baselineContext != null) {
            context.getCloudflare().setR2Bucket(baselineContext.getCloudflare().getR2Bucket());
        } else {
            context.getCloudflare().setR2Bucket(appProperties.getStorage().getR2().getBucket());
        }

        return context;
    }
}
