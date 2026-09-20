# Vireo — Current Context

This file captures the live state of the project: what is decided, what is in progress, and what is next.
**Update this file** at the end of every working session or when a decision is made.

---

## Current Phase

**Phase 5 — CLI Improvements & Init Wizard `[completed]`**
Implemented `kubectl`-style CLI ergonomics, per-command `--help` (`vireo render --help`, `vireo check --help`, `vireo init --help`), format selection flags (`-o, --output <format>`, `--html`, `--figma`), HTML parameters (`HtmlRenderOptions` with standard HTML5 document vs embeddable `--snippet`, custom `--title`, preview `--theme`), Figma parameters (`--figma`, `--token`, `--pretty`, `--compact`), `vireo init` project scaffolding wizard, comprehensive reference in `DOCS/cli.md`, and Unlazy verified acceptance gates in `GATES.md`. Next is Phase 6 — Release Engineering (0.x → 1.0.0).

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
| Kubectl-style CLI Ergonomics (5) | Standardized CLI with `-o <format>`, convenience `--html`/`--figma`, per-command `--help`, version command | 2026-09-20 |
| HTML Render Options (5) | Added `HtmlRenderOptions` supporting standard HTML5 full page vs embeddable `--snippet`, `--title`, `--theme` | 2026-09-20 |
| Init Project Wizard (5) | Implemented `vireo init` scaffolding project structure with tokens, components, card, config, and README | 2026-09-20 |
| Versioning & release gate | Lockstep SemVer across all Gradle modules, starting at `0.x`. No public release before `1.0.0`; `1.0.0` requires the `.dac` syntax freeze (see `SYNTAX.md` open questions) | 2026-09-20 |
| CLI distribution channel | Homebrew is the target channel; bootstrap with GitHub Releases (fat jar) first, add a Homebrew tap once the release pipeline is proven | 2026-09-20 |
| Figma wire-schema versioning | Added `schemaVersion` (default `1`) to `FigmaDocument`/`*.figma.json` (`vireo-renderer-figma`), plus a matching `SUPPORTED_SCHEMA_VERSION` check in the plugin's `code.js` that errors clearly instead of silently mis-rendering on a mismatch | 2026-09-20 |
| Versioning playbook | Documented explicit PATCH/MINOR/MAJOR bump rules for the `0.x` beta stage and post-`1.0.0` in `ARCHITECTURE.md`, with the mandatory gate (`1.0.0` only on explicit user command) in `BLOCK.md` | 2026-09-20 |

---

## Open Decisions (blocking progress)

*None at this time.*

---

## What Was Done Last Session

- **Phase 5 — CLI Improvements & Init Wizard**:
  - Implemented `HtmlRenderOptions` in `vireo-renderer-html`:
    - Full standard HTML5 document mode (`<!DOCTYPE html>`, `<html lang="en">`, `<head>`, `<meta charset>`, viewport, `<title>`, and centered preview container).
    - Embeddable component fragment / snippet mode (`--snippet`, omitting outer DOCTYPE/head/body wrappers).
    - Custom `--title` and preview background `--theme` (`light` vs `dark`).
    - Added unit tests in `HtmlRendererTest.kt`.
  - Implemented `kubectl`-style CLI in `vireo-cli`:
    - First-class per-command help: `vireo render --help`, `vireo check --help`, `vireo init --help`, `vireo version`.
    - Output format flags: `-o, --output <format>` (`json`, `html`, `figma`, `standard-html`), plus backward-compatible `--to <format>`.
    - Convenience format flags: `--html`, `--figma`, `--json`.
    - Destination output flags: `--out <file>`, `--output-file <file>`, and smart `-o <file>` fallback.
    - Figma options: `--pretty` (default) vs `--compact` minified AST JSON, and `--token`.
    - Global `vireo version` command.
  - Implemented Phase 5 `vireo init` Project Wizard:
    - Scaffolds a complete project with `vireo.config.json`, `designs/tokens.dac`, `designs/components/button.dac`, `designs/card.dac`, and `README.md`.
    - Tested that the generated project passes `vireo check` and renders with `vireo render --html`.
  - Updated `DOCS/cli.md` with complete reference manual, option tables, and examples.
  - Authored and verified unlazy acceptance gates ledger `GATES.md` (all 7 gates met with machine evidence).

---

## What Is Next

1. Phase 6 — Release Engineering (0.x → 1.0.0): syntax freeze, CI, release workflow, `schemaVersion` for `*.figma.json` + plugin version check, `CHANGELOG.md`, GitHub Releases → Homebrew tap.
2. Backlog Item: Figma Design Bundle & Canvas Orchestrator (`vireo bundle`) — package multiple `.dac` files into a single consolidated canvas bundle with automated grid layout and Figma Sections/Pages.
3. Parked, long-term: Code Generation (`ResolvedFile → UI code`) — deliberately not a numbered phase; revisit after `1.0.0` and after the bundle backlog item.
4. Internal refactor (temporary, side track — not a roadmap phase): `.agents/rules/REFACTOR_PLAN.md` — Phase 2 (`vireo-lexer`) is next.

---

## Active Constraints

- `vireo-core` must not use `java.io` or `java.net`
- Every new AST node must have a `SourceLocation` field
- Syntax proposals go in `SYNTAX.md`, not in code comments
- No public release ships before `1.0.0` — that requires the `.dac` syntax freeze (open questions in `SYNTAX.md`), not just feature completeness