---
name: RAG Chunking Checklist
description: Validate fixed pre-chunking + semantic merge + threshold + metadata completeness before implementing chunking changes.
argument-hint: Describe target files/modules and intended chunking strategy change
agent: agent
tools: [read, search]
---

Run a pre-implementation checklist for RAG chunking changes in this workspace.

Focus on four mandatory dimensions:

1. Fixed-size pre-chunking exists and has explicit size policy.
2. Semantic merge/refinement logic exists and explains adjacency/similarity behavior.
3. Threshold strategy is explicit (default, range, fallback behavior).
4. Metadata is complete for each chunk (at minimum source doc id and chunk id).

Output format:

- Scope scanned (files and modules)
- Checklist result:
  - fixed pre-chunking: pass/fail + reason
  - semantic merge: pass/fail + reason
  - threshold policy: pass/fail + reason
  - metadata completeness: pass/fail + reason
- Gaps to fix first (ordered by risk)
- Minimal implementation plan (3-6 steps)

Rules:

- If project code is not scaffolded yet, report "not initialized" and provide a bootstrap-ready checklist target map for `backend/` and `frontend/`.
- Do not invent missing files.
