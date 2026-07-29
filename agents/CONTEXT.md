# Vireo — Current Context

This file captures the live state of the project: what is decided, what is in progress, and what is next.
**Update this file** at the end of every working session or when a decision is made.

---

## Current Phase

**Phase 0 — Foundation `[completed]`**
Scaffold created and validated with `./gradlew build`. All foundational architecture decisions made.

---

## Decisions Made

| Topic | Decision | Date |
|-------|----------|------|
| Implementation language | Kotlin (pure JVM, no KMP) | 2026-07-29 |
| AST contract | Stable interface between all stages | 2026-07-29 |
| Layout model | Hybrid: Auto Layout + Explicit + Relational | 2026-07-29 |
| Component addressing | `file.block.component` dot notation | 2026-07-29 |
| Error handling | `VireoResult<T>` — no exceptions | 2026-07-29 |
| AST traversal | Visitor pattern (`AstVisitor<T>`) | 2026-07-29 |
| Browser tooling | Separate language (JS/TS) — not KMP | 2026-07-29 |
| Init wizard | CLI first, GUI later | 2026-07-29 |
| Build tool | Gradle with Kotlin DSL (`build.gradle.kts`) | 2026-07-29 |
| Parser strategy | Hand-written recursive descent | 2026-07-29 |
| Expression evaluator | Custom AST mini-evaluator | 2026-07-29 |

---

## Open Decisions (blocking progress)

| Decision | Recommendation | Blocks |
|----------|----------------|--------|
| Figma transport | Figma REST API | Phase 4 |

---

## What Was Done Last Session

- Decided build tool (Gradle with Kotlin DSL), parser strategy (Recursive descent), and expression evaluator (Custom mini-evaluator)
- Logged decisions in `ARCHITECTURE.md` Decision Log
- Scaffolded Gradle multi-module project with empty modules: `vireo-core`, `vireo-lexer`, `vireo-parser`, `vireo-analysis`, `vireo-renderer-json`, `vireo-renderer-html`, `vireo-renderer-figma`, `vireo-cli`
- Generated Gradle wrapper and verified compilation with `./gradlew build`

---

## What Is Next

1. Implement `vireo-core` — AST nodes and shared types (`VireoResult`, `SourceLocation`, `AstVisitor`)
2. Implement `vireo-lexer` — basic tokenization
3. Implement `vireo-parser` — parse blocks, components, and explicit properties

---

## Active Constraints

- `vireo-core` must not use `java.io` or `java.net`
- Every new AST node must have a `SourceLocation` field
- Syntax proposals go in `SYNTAX.md`, not in code comments