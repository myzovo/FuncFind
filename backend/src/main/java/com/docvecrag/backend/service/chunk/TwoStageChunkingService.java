package com.docvecrag.backend.service.chunk;

import com.docvecrag.backend.model.ChunkRecord;
import com.docvecrag.backend.model.StoredDocument;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TwoStageChunkingService {

    public List<ChunkRecord> chunkDocuments(List<StoredDocument> docs, String kbName, int chunkSize,
            double semanticThreshold) {
        List<ChunkRecord> result = new ArrayList<>();
        for (StoredDocument doc : docs) {
            List<String> stageAChunks = coarseSplit(doc.getRawText(), chunkSize);
            List<String> stageBChunks = semanticRefine(stageAChunks, chunkSize, semanticThreshold);
            Instant ingestedAt = doc.getCreatedAt() == null ? Instant.now() : doc.getCreatedAt();
            for (String text : stageBChunks) {
                result.add(new ChunkRecord(
                        UUID.randomUUID().toString(),
                        doc.getDocumentId(),
                        kbName,
                        text,
                        ingestedAt));
            }
        }
        return result;
    }

    private List<String> coarseSplit(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private List<String> semanticRefine(List<String> chunks, int chunkSize, double threshold) {
        List<String> refined = new ArrayList<>();
        if (chunks.isEmpty()) {
            return refined;
        }

        String current = chunks.get(0);
        for (int i = 1; i < chunks.size(); i++) {
            String next = chunks.get(i);

            double similarity = tokenOverlap(current, next);
            boolean shareStructure = endsWithOpenStructure(current);
            boolean withinSizeBudget = (current.length() + next.length()) <= (int) (chunkSize * 1.6);

            if (withinSizeBudget && (similarity >= threshold || shareStructure)) {
                current = current + "\n" + next;
            } else {
                refined.add(current.trim());
                current = next;
            }
        }

        refined.add(current.trim());
        return refined;
    }

    private boolean endsWithOpenStructure(String text) {
        String normalized = text == null ? "" : text.trim();
        return normalized.endsWith(":") || normalized.endsWith(";") || normalized.endsWith("，")
                || normalized.endsWith(",");
    }

    private double tokenOverlap(String left, String right) {
        String[] leftTokens = normalize(left).split(" ");
        String[] rightTokens = normalize(right).split(" ");

        if (leftTokens.length == 0 || rightTokens.length == 0) {
            return 0.0;
        }

        int overlap = 0;
        for (String l : leftTokens) {
            if (l.isBlank()) {
                continue;
            }
            for (String r : rightTokens) {
                if (l.equals(r)) {
                    overlap++;
                    break;
                }
            }
        }

        int denom = Math.max(leftTokens.length, rightTokens.length);
        if (denom == 0) {
            return 0.0;
        }
        return Math.min(1.0, overlap / (double) denom);
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
