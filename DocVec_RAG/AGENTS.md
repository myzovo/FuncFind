# AGENTS.md

## Scope

- This repository currently contains planning docs, an initial frontend prototype, and a backend skeleton.
- Use [Prompt](Prompt) as the source of truth for product and architecture goals.
- Do not implement unrelated features outside the RAG scope described in [Prompt](Prompt).

## Current Repository State

- Present files: [Prompt](Prompt), [README.MD](README.MD), [ARCHITECTURE.md](ARCHITECTURE.md), [backend/API_CONTRACT.md](backend/API_CONTRACT.md), [frontend/](frontend/), [backend/](backend/).
- Missing (not initialized yet): Docker files, CI files.

## Expected Architecture (When Bootstrapping)

- `backend/`: Java Spring Boot APIs for ingestion, retrieval orchestration, and answer generation flow.
- `frontend/`: Vue + JavaScript app for document upload, question input, and result display.
- `docker/` or root compose files: local containerized startup for core services.
- Keep model or embedding service boundaries explicit (in-process or sidecar), and document the chosen mode.

## Agent Working Rules

- Before coding, confirm target module (`backend` or `frontend`) and list affected interfaces.
- Keep changes modular by workflow stage: upload/extract, chunking, embedding, indexing, retrieval, generation.
- Prefer incremental PR-sized edits with runnable checkpoints.
- Preserve lightweight deployment goals from [Prompt](Prompt): minimize memory and compute overhead.

## RAG-Specific Constraints

- Chunking should follow a hybrid strategy (size-based pre-chunking + semantic refinement) as specified in [Prompt](Prompt).
- Retrieval should favor high-relevance context and predictable top-k behavior; avoid hidden heuristics without documentation.
- Prompt assembly for generation must clearly separate user question and retrieved context blocks.
- Keep metadata with every chunk (at minimum source doc id, chunk id, and ingestion timestamp).
- Vector DB baseline is Cloudflare Vectorize; object/intermediate text storage should be R2 or backend persistence layer.
- Chunking implementation should stay two-stage: fixed-length coarse split, then semantic/structure-based merge or split.
- Embedding must be identical for indexing and retrieval (same model and same dimension).
- Backend must orchestrate `/documents/upload`, `/knowledge-base/build`, and `/chat`; frontend should focus on upload UX, parameter controls, and result display.
- Embedding provider must remain swappable (Cloudflare Workers AI or independent model service).

## Build/Test Commands

- Backend build tool preference: Maven (prefer `mvnw` when bootstrapped).
- Backend build/test/run:
  - Windows PowerShell: `cd backend; .\mvnw.cmd clean package; .\mvnw.cmd test; .\mvnw.cmd spring-boot:run`
  - macOS/Linux: `cd backend && ./mvnw clean package && ./mvnw test && ./mvnw spring-boot:run`
- Frontend run (current static prototype): open `frontend/index.html` directly, or serve `frontend/` via static server.
- Docker compose commands: pending until compose files are added.

## Documentation Policy

- Link to existing docs instead of duplicating large instructions.
- If new docs are added later (for example architecture or contribution guides), reference them from this file and keep this file concise.
- Keep implementation decisions synchronized with [ARCHITECTURE.md](ARCHITECTURE.md).
- Keep backend API and metadata contracts synchronized with [backend/API_CONTRACT.md](backend/API_CONTRACT.md).

## Safe Defaults Until Stack Is Initialized

- Backend default assumption: Spring Boot on port 8080.
- Frontend default assumption: Vue dev server on port 5173.
- Treat these as placeholders; update once actual config files exist.
