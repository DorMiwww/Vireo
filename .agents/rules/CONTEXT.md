# Vireo — Current Context

This file captures the live state of the project: what is decided, what is in progress, and what is next.
**Update this file** at the end of every working session or when a decision is made.

---

## Current Phase

**Phase 4.5 — Figma Plugin `[completed]`**
Implemented `figma-plugin/` private development plugin (`manifest.json`, `ui.html`, `code.js`, `README.md`, and guide in `DOCS/figma-plugin.md`). Automatically imports `*.figma.json` and renders native Figma canvas frames, auto-layout constraints, fonts, text layers, shapes, fills, and strokes. Next is Phase 5 — Init Wizard.

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
| Parser strategy | Hand-written recursive descent producing unresolved AST | 2026-07-29 |
| Expression evaluator | Custom AST mini-evaluator | 2026-07-29 |
| Lexer architecture | Stateless, returns `VireoResult<List<Token>>` (never throws) | 2026-07-29 |
| Parser architecture | Hand-written recursive descent, returns `VireoResult<VireoFile>`, collects all errors with `SourceLocation` | 2026-07-29 |
| JSON Renderer architecture | Implements `Renderer<String>`, emits structured JSON for `VireoFile` and `ResolvedFile` | 2026-07-29 |
| CLI Architecture | Manual argument parsing, `executeCli(args, out, err): Int` returning exit code 0/1 | 2026-07-29 |
| End-to-end Validation | Verified Example 1 & 2 from `EXAMPLES.md` render to correct JSON via CLI | 2026-07-31 |
| Lexer Extensions (2-A) | Added explicit lexer support and tests for `import` keyword, `from` keyword, and `.` dot notation | 2026-07-31 |
| Parser Extensions (2-B) | Added explicit parser support and tests for `import` declarations and `ref:` cross-file references | 2026-07-31 |
| Analyzer Implementation (2-C) | Implemented `vireo-analysis` Analyzer stage with recursive file loading, reference resolution, cycle detection, error accumulation, and `vireo check` CLI command | 2026-07-31 |
| Layout Constraints Parsing (2-D) | Audited and completed `vireo-parser` parsing for `Constraint.Explicit`, `Constraint.AutoLayout`, and `Constraint.Relational` with comprehensive unit tests | 2026-07-31 |
| HTML Renderer Implementation (2-E) | Implemented pure, stateless `HtmlRenderer : Renderer<String>` in `vireo-renderer-html` mapping AST components to HTML `div`s with inline CSS Flexbox, explicit dimensions, relational calcs, and visual styling. Added unit tests for Example 3 & 4 | 2026-07-31 |
| CLI Extensions (2-F) | Extended `vireo-cli` with `vireo render <file.dac> --to html` (supporting stdout & `-o` file output) and `vireo check <file.dac>` error formatting with `file:line:col` | 2026-07-31 |
| End-to-End Tests (2-G) | Added comprehensive CLI e2e integration tests verifying Example 3 (Auto Layout Card) and Example 4 (Cross-file Login Form) render to valid JSON and HTML via CLI | 2026-07-31 |
| Lexer Extensions (3-A) | Added `DOUBLE_EQUALS` (`==`) token type and scanning support in `vireo-lexer` with unit tests for `var`, `fun`, `if`, `then`, `else`, `$`, `==` | 2026-08-01 |
| Var Declarations (3-B) | Added `VarDeclaration` AST node in `vireo-core` and top-level `var` parsing in `vireo-parser` with unit tests | 2026-08-01 |
| Fun Declarations (3-C) | Added `Parameter` and `FunDeclaration` AST nodes in `vireo-core` and top-level `fun` parsing in `vireo-parser` with unit tests | 2026-08-01 |
| Conditionals Parsing (3-D) | Added `PropertyValue.ConditionalExpr` AST node in `vireo-core` and `if ... then ... else` conditional parsing in `vireo-parser` with unit tests | 2026-08-01 |
| Expression Evaluator (3-E) | Implemented `ExprEvaluator` in `vireo-analysis` for variable substitution, arithmetic, function calls, relational offset evaluation, conditionals, and string interpolation, with full unit test coverage | 2026-08-01 |
| End-to-End Tests (3-F) | Added CLI end-to-end integration tests for Example 5 (Variables) and Example 6 (Relational Constraints) rendering to JSON & HTML | 2026-08-01 |
| Figma Transport Decision | Recorded decision for Figma REST API transport in `ARCHITECTURE.md` | 2026-08-01 |
| Figma Renderer Implementation | Implemented `FigmaRenderer` pure renderer in `vireo-renderer-figma` mapping `ResolvedFile` to Figma REST API `FigmaDocument` JSON nodes | 2026-08-01 |
| CLI Figma Command | Extended `vireo-cli` with `vireo render <file.dac> --to figma [--token <token>] [-o <file.json>]` using OkHttp transport | 2026-08-01 |
| Figma Development Plugin (4.5) | Private development plugin (JS/HTML) reading `*.figma.json` to draw designs natively on Figma canvas | 2026-09-20 |

---

## Open Decisions (blocking progress)

*None at this time.*

---

## What Was Done Last Session

- **Phase 4.5 — Figma Plugin & Showcase Verification**:
  - Implemented private Figma development plugin in `figma-plugin/`:
    - `manifest.json`: Figma development plugin manifest with `id`, `api: "1.0.0"`, `main: "code.js"`, `ui: "ui.html"`.
    - `ui.html`: Polished UI supporting drag-and-drop file upload, file browsing for `*.figma.json`, and direct JSON pasting.
    - `code.js`: Sandbox engine converting AST nodes (`CANVAS`, `FRAME`, `TEXT`, `RECTANGLE`) to native Figma nodes via `figma.createFrame`, `figma.createText`, font loading (`Inter` Regular/Bold), Auto Layout parameters (`layoutMode`, gaps, padding, axis sizing), fills, strokes, and corner radiuses.
    - `README.md`: Quick reference in `figma-plugin/`.
  - Created end-to-end guide in `DOCS/figma-plugin.md`.
  - Verified node mapping against `designs/login.figma.json` using sandbox simulation test.
  - Implemented comprehensive showcase designs in `showcase/`: `components/buttons.dac`, `components/badges.dac`, `card.dac`, `pricing.dac`, and a large multi-section SaaS `dashboard.dac` (261 KB, 3,428 lines of Figma nodes) with full `.html` and `.figma.json` rendering.
  - Fixed compiler & renderer bugs: string interpolation with price literals (`$19`), Auto Layout `layoutGrow = 1f` / `flex: 1` in horizontal containers, and `justifyContent` / `alignItems` / `backgroundColor` properties.

---

## What Is Next

1. Phase 5 — Init Wizard: implement `vireo init` interactive project scaffolding.
2. Backlog Item: Figma Design Bundle & Canvas Orchestrator (`vireo bundle`) — package multiple `.dac` files into a single consolidated canvas bundle with automated grid layout and Figma Sections/Pages.
3. Internal refactor (temporary, side track — not a roadmap phase): `.agents/rules/REFACTOR_PLAN.md` — module-by-module file-splitting and dedup plan, test-first per phase, 8 phases not yet started. Pick up the first `[not started]` phase in that file when resuming this work.

---

## Active Constraints

- `vireo-core` must not use `java.io` or `java.net`
- Every new AST node must have a `SourceLocation` field
- Syntax proposals go in `SYNTAX.md`, not in code comments