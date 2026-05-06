# Architecture Decisions

Last updated: 2026-04-17

This file records implementation-level decisions that refine but do not replace [Prompt](Prompt).

## Decision Baseline

1. Vector storage is Cloudflare Vectorize.
2. Original files and intermediate text are stored in Cloudflare R2 or backend persistence, with clear responsibility separation from vector indexing.
3. A compact text-processing model is orchestrated by backend before chunking, for cleaning, title detection, paragraph compression, or key-sentence extraction.
4. Chunking is two-stage:

- Stage A: fixed-length coarse split for predictable indexing behavior.
- Stage B: semantic merge/split based on similarity and structure boundaries.

5. Embedding must be identical for indexing and retrieval, including model identity and vector dimension.
6. Backend orchestrates `/documents/upload`, `/knowledge-base/build`, and `/chat` as one coherent flow.
7. Frontend is limited to upload UX, parameter configuration, and result rendering.
8. Embedding service remains switchable between Cloudflare Workers AI and an independent model service.
9. Requirements mainline remains [Prompt](Prompt), frontend conventions remain [README.MD](README.MD), and module boundaries remain [AGENTS.md](AGENTS.md).

## Implementation Notes

- Keep a single embedding configuration source in backend runtime settings.
- Validate embedding dimension at startup and before index/query operations.
- Persist chunk metadata at minimum: source doc id, chunk id, and ingestion timestamp.
- Keep retrieval top-k behavior deterministic and documented.
- Runtime provider credentials/config are session-scoped and kb-scoped via `X-Client-Session-Id + kbName`.
- Secrets are applied only through dedicated runtime-config APIs and always masked in read responses.

## Rollout Order (Frozen)

To avoid Vectorize index rebuild caused by embedding dimension drift, use this order:

1. Freeze contracts first.
2. Replace raw/intermediate text storage (R2 or backend persistence).
3. Replace embedding/generation placeholders.
4. Replace Vectorize stub with Cloudflare API integration.
5. Close frontend integration with real backend path and controlled fallback switch.

Contract freeze reference: [backend/API_CONTRACT.md](backend/API_CONTRACT.md)
