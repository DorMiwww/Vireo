# Vireo — Current Context

This file captures the live state of the project: what is decided, what is in progress, and what is next.
**Update this file** at the end of every working session or when a decision is made.

---

## Current Phase

## Current Phase

**Phase 3 — Language Features `[in progress]`**
Completed `vireo-lexer` audit & extension (`Task 3-A`), `vireo-parser` `var` declarations (`Task 3-B`), `vireo-parser` `fun` declarations (`Task 3-C`), and `vireo-parser` conditionals (`Task 3-D`). AST nodes (`VarDeclaration`, `FunDeclaration`, `Parameter`, `ConditionalExpr`) added to `vireo-core` and `AstVisitor<T>`, `JsonRenderer`, `HtmlRenderer`, and `Analyzer` updated. Next is Task 3-E (Expression Evaluator).

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
| Visitor & Renderer Exhaustiveness | Extended `AstVisitor<T>` with `visitVarDeclaration` and `visitFunDeclaration`, and updated `JsonRenderer`, `HtmlRenderer`, and `Analyzer` | 2026-08-01 |

---

## Open Decisions (blocking progress)

| Decision | Recommendation | Blocks |
|----------|----------------|--------|
| Figma transport | Figma REST API | Phase 4 |

---

## What Was Done Last Session

- **Task 3-A (vireo-lexer audit & extension)**:
  - Added `DOUBLE_EQUALS` (`==`) token type in `Token.kt` and scanner support in `Lexer.kt`.
  - Added unit test cases in `LexerTest.kt` verifying `var`, `fun`, and conditional statements with `==`.
- **Task 3-B (vireo-parser var declarations)**:
  - Defined `VarDeclaration` AST node in `vireo-core` with mandatory `SourceLocation`.
  - Updated `VireoFile` to hold top-level `vars: List<VarDeclaration>`.
  - Implemented top-level `var` declaration parsing in `Parser.kt` and added unit tests in `ParserTest.kt`.
- **Task 3-C (vireo-parser fun declarations)**:
  - Defined `Parameter` and `FunDeclaration` AST nodes in `vireo-core` with mandatory `SourceLocation`.
  - Updated `VireoFile` to hold top-level `functions: List<FunDeclaration>`.
  - Implemented top-level `fun` declaration parsing (`fun <name>(<param>: <type>, ...): <retType> { [return] <expr> }`) in `Parser.kt` and added unit tests in `ParserTest.kt`.
- **Task 3-D (vireo-parser conditionals)**:
  - Extended `PropertyValue` sealed class with `PropertyValue.ConditionalExpr(condition, thenBranch, elseBranch, location)`.
  - Implemented conditional expression parsing (`if ... then ... else ...`) in `Parser.kt` and added unit tests in `ParserTest.kt`.
- **AstVisitor<T> & Renderers Exhaustiveness**:
  - Updated `AstVisitor<T>` with `visitVarDeclaration` and `visitFunDeclaration`.
  - Updated `JsonRenderer` to serialize `vars`, `functions`, and `ConditionalExpr`.
  - Updated `HtmlRenderer` and `Analyzer` for the new AST nodes.
- **Verification**:
  - Ran `./gradlew test` across all modules — 100% of tests passed cleanly.

---

## What Is Next

1. Task 3-E: Implement expression evaluator — arithmetic, string interpolation, basic conditionals (`ExprEvaluator` in `vireo-analysis`)

---

## Active Constraints

- `vireo-core` must not use `java.io` or `java.net`
- Every new AST node must have a `SourceLocation` field
- Syntax proposals go in `SYNTAX.md`, not in code comments