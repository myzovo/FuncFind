---
name: repo-bootstrap
description: "Bootstrap an empty DocVec_RAG-style workspace with Maven-first Spring Boot backend, Vue frontend, Docker skeleton, and then backfill AGENTS build/test commands."
argument-hint: "minimal|standard, include-ci yes|no, model-service in-process|sidecar"
user-invocable: true
---

# Repo Bootstrap (Maven-First)

Use this skill when the repository is empty or only has planning docs, and you need a repeatable initialization flow.

## Expected Outcome

- A runnable project skeleton with three modules: `backend/`, `frontend/`, and Docker startup files.
- Maven-first backend setup (no Gradle unless explicitly requested).
- AGENTS instructions updated with real build/test/run commands after scaffolding.

## Inputs

- `minimal` or `standard`: controls how much starter code is generated.
- `include-ci yes|no`: whether CI workflow files are included.
- `model-service in-process|sidecar`: embedding/model execution boundary.

## Procedure

1. Read [Prompt](../../../Prompt) and [AGENTS.md](../../../AGENTS.md) to lock scope and constraints.
2. Detect current repository state.
   If `backend/` and `frontend/` already exist, do not re-scaffold; switch to command backfill and consistency checks.
3. Initialize backend in `backend/` using Maven conventions.
   Prefer Maven Wrapper (`mvnw`) and Spring Boot defaults.
4. Initialize frontend in `frontend/` with Vue + JavaScript tooling.
5. Add Docker startup files in `docker/` or root compose, with explicit service boundaries.
6. Backfill [AGENTS.md](../../../AGENTS.md) Build/Test section with exact commands for backend, frontend, and Docker.
7. Verify constraints from [Prompt](../../../Prompt):

- Hybrid chunking flow is represented in module boundaries.
- Retrieval keeps predictable top-k behavior.
- Chunk metadata includes source doc id + chunk id.

8. Return a concise report of created files, commands, and any TODO items.

## Decision Points

- Backend build tool: Maven by default.
  Only switch away from Maven if user explicitly requests it.
- Model execution boundary:
  Use `sidecar` when Python embedding stack is separated; use `in-process` only when dependencies are fully managed in backend runtime.
- Existing files conflict:
  Preserve existing user content and do additive changes.

## Quality Checks

- `backend/`, `frontend/`, and Docker startup artifacts exist.
- AGENTS Build/Test commands are concrete and executable.
- No unrelated features are introduced beyond RAG bootstrap scope.
- Documentation links point to source docs instead of duplicating long text.

## Completion Checklist

- [ ] Scaffolding created or safely skipped due to existing modules
- [ ] Maven-first backend choice applied
- [ ] AGENTS commands backfilled
- [ ] Open TODOs clearly listed
