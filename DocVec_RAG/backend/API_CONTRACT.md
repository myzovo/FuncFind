# Backend API Contract (Frozen)

Last updated: 2026-04-17

This document freezes the minimum API and data contract before replacing storage/model/vector integrations.

## Frozen Rules

1. Indexing and query embedding must use the same model and same dimension.
2. Minimum chunk metadata fields are fixed as:

- `chunkId`
- `sourceDocId`
- `ingestedAt`

3. Core orchestration endpoints remain:

- `POST /api/documents/upload`
- `POST /api/knowledge-base/build`
- `POST /api/chat`

4. Runtime provider/credential settings are session-scoped and kb-scoped via header + config APIs:

- Header: `X-Client-Session-Id`
- Config APIs: `POST /api/runtime-config/apply`, `GET /api/runtime-config/current`

5. Sensitive values (API Token / API Key) are accepted only through runtime-config APIs and must be masked in responses.

## Endpoint Contracts

### POST /api/documents/upload

- Content-Type: `multipart/form-data`
- Header:
  - `X-Client-Session-Id` (optional, recommended)
- Form fields:
  - `file` (required)
  - `kbName` (optional, default `default-kb`)
- Response fields:
  - `documentId`
  - `kbName`
  - `filename`
  - `createdAt`
  - `size`
  - `status`

### POST /api/knowledge-base/build

- Content-Type: `application/json`
- Header:
  - `X-Client-Session-Id` (optional, recommended)
- Request fields:
  - `kbName` (required)
  - `chunkSize` (optional)
  - `semanticThreshold` (optional)
  - `topK` (optional)
  - `embeddingModel` (optional but must match runtime config if provided)
  - `sourceDocs` (optional)
- Response fields:
  - `kbName`
  - `documentCount`
  - `indexedChunkCount`
  - `embeddingModel`
  - `embeddingDimension`
  - `status`
  - `message`

### POST /api/knowledge-base/build-from-crawl

- Content-Type: `application/json`
- Header:
  - `X-Client-Session-Id` (optional, recommended)
- Request fields:
  - `kbName` (required)
  - `embeddingModel` (optional but must match runtime config if provided)
  - `siteId` (optional)
  - `pages[]` (required)
    - `url` (optional)
    - `title` (optional)
    - `elements[]`
      - `type` (optional)
      - `text` (optional)
      - `href` (optional)
      - `selector` (optional)
      - `context` (optional)
      - `contextText` (optional)
      - `nearestHeading` (optional)
      - `tag` (optional)
      - `role` (optional)
      - `xpath` (optional)
- Response fields:
  - same as `POST /api/knowledge-base/build`

### POST /api/chat

- Content-Type: `application/json`
- Header:
  - `X-Client-Session-Id` (optional, recommended)
- Request fields:
  - `question` (required)
  - `kbName` (required)
  - `topK` (optional)
  - `generationModel` (optional)
- Response fields:
  - `answer`
  - `kbName`
  - `generationModel`
  - `retrievedCount`
  - `contextChunks[]` with:
    - `chunkId`
    - `sourceDocId`
    - `ingestedAt`
    - `score`
    - `text`
    - `pageUrl` (optional)
    - `pageTitle` (optional)
    - `elementText` (optional)
    - `elementType` (optional)
    - `selector` (optional)
    - `href` (optional)
    - `context` (optional)
    - `evidence` (optional)
    - `contextText` (optional)
    - `nearestHeading` (optional)
    - `tag` (optional)
    - `role` (optional)
    - `xpath` (optional)
  - `locations[]` (optional) with:
    - `pageUrl`
    - `selector`
    - `evidence`
    - `xpath` (optional)
    - `elementText` (optional)

## Runtime Config APIs

Session runtime config is scoped by: `X-Client-Session-Id + kbName`.

### POST /api/runtime-config/apply

- Header:
  - `X-Client-Session-Id` (required)
- Content-Type: `application/json`
- Request fields:
  - `kbName` (required)
  - `cloudflare.accountId` (optional)
  - `cloudflare.apiToken` (optional, sensitive)
  - `cloudflare.vectorizeIndexName` (optional)
  - `cloudflare.vectorizeNamespace` (optional)
  - `cloudflare.r2Bucket` (optional)
  - `generation.providerType` (optional, `workers-ai|external`)
  - `generation.baseUrl` (optional)
  - `generation.apiKey` (optional, sensitive)
  - `generation.generationModelId` (optional)
- Response fields:
  - `sessionId`
  - `kbName`
  - `appliedAt`
  - `cloudflare.accountId`
  - `cloudflare.vectorizeIndexName`
  - `cloudflare.vectorizeNamespace`
  - `cloudflare.r2Bucket`
  - `cloudflare.hasApiToken`
  - `cloudflare.apiTokenMasked`
  - `generation.providerType`
  - `generation.baseUrl`
  - `generation.generationModelId`
  - `generation.hasApiKey`
  - `generation.apiKeyMasked`

### GET /api/runtime-config/current

- Header:
  - `X-Client-Session-Id` (required)
- Query:
  - `kbName` (optional, default `default-kb`)
- Response fields:
  - same as `POST /api/runtime-config/apply`
- Secret behavior:
  - secret raw values are never returned
  - only `hasApiToken/hasApiKey` and masked previews are returned

## Locate Tracking API

### POST /api/locate

- Content-Type: `application/json`
- Header:
  - `X-Client-Session-Id` (optional)
- Request fields:
  - `kbName` (required)
  - `status` (required, `failed|success`)
  - `pageUrl` (optional)
  - `selector` (optional)
  - `xpath` (optional)
  - `elementText` (optional)
  - `method` (optional)
  - `reason` (optional)
  - `details` (optional)
- Response fields:
  - `status` (`logged`)

## Operational Endpoints

These endpoints are for observability and recovery during storage/model migration.

### GET /api/storage/fallback/events

- Query:
  - `limit` (optional)
- Response list fields:
  - `eventId`
  - `status`
  - `documentId`
  - `kbName`
  - `filename`
  - `fallbackReason`
  - `targetR2Bucket`
  - `replayAttempts`
  - `occurredAt`
  - `lastReplayAt`
  - `lastError`

### POST /api/storage/fallback/replay

- Header:
  - `X-Client-Session-Id` (optional)
- Request fields:
  - `kbName` (optional)
  - `limit` (optional)
- Response fields:
  - `attempted`
  - `replayed`
  - `failed`
  - `pendingAfter`

### GET /api/models/routing/status

- Header:
  - `X-Client-Session-Id` (optional)
- Query:
  - `kbName` (optional)
- Response fields:
  - `selectedAdapter`
  - `preferredAdapter`
  - `defaultAdapter`
  - `fallbackAdapter`
  - `defaultHealthy`
  - `fallbackHealthy`

## Rollout Order

1. Freeze contracts and dimensions (this phase).
2. Replace raw/intermediate storage with R2 or backend persistence.
3. Replace embedding/generation placeholders with Workers AI or external model service.
4. Replace Vectorize stub with Cloudflare API integration.
5. Close frontend integration by disabling demo path behind a controlled fallback switch.
