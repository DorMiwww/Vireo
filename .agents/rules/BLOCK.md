# Vireo — Block Rules

Mandatory rules for every AI agent working on this project.
Read before touching any file. These override general coding instincts.

---

## Language & Platform

- **Kotlin only.** No Java, no Groovy, no other JVM languages.
- **Pure Kotlin JVM.** Do not introduce KMP (Kotlin Multiplatform) targets without explicit user approval.
- **`vireo-core` must stay platform-neutral** — no `java.io`, `java.net`, or any JVM-specific API inside core. Other modules may use JVM APIs freely.

---

## Architecture Rules

- **Follow the module dependency direction.** Outer modules depend on inner ones — never the reverse.
  ```
  vireo-cli → vireo-renderer-* → vireo-analysis → vireo-parser → vireo-lexer → vireo-core
  ```
- **Never create a dependency from an inner module to an outer one.** `vireo-core` must not import from `vireo-parser`. `vireo-parser` must not import from `vireo-analysis`. Violations break the entire decoupling.
- **`vireo-idea-plugin` is a standalone Gradle project, not part of this multi-module build.** It lives as a sibling directory (like `figma-plugin/`) with its own Gradle wrapper (IntelliJ Platform Gradle Plugin 2.x requires Gradle 9.0+ and Kotlin 2.x — incompatible with the root build's Gradle 8.5 / Kotlin 1.9.22, and composite-build (`includeBuild`) across that version gap was judged too risky). It consumes `vireo-core`/`vireo-lexer`/`vireo-parser`/`vireo-analysis` as plain jar files built from this repo (`./gradlew :vireo-lexer:jar` etc.), not as live `project(...)` dependencies — rebuild and re-copy those jars after changing any of the four modules.
- **After changing `vireo-core`/`vireo-lexer`/`vireo-parser`/`vireo-analysis`, the plugin does not pick it up automatically.** Run `vireo-idea-plugin/sync-libs.sh`, then rebuild the plugin (`./gradlew buildPlugin` or `runIde`). If it's installed in a real IDE (not just `runIde`'s sandbox), reinstall the new zip via Install Plugin from Disk again — updated jars alone don't reach an already-installed plugin.
- **The AST is the stable contract.** All stages communicate through AST nodes defined in `vireo-core`. Do not bypass the AST by passing raw strings or ad-hoc structures between stages.
- **Renderers are pure and stateless.** A renderer must not perform I/O (no file reads, no network calls). It receives a `ResolvedFile`, returns a `VireoResult<T>`. The CLI handles all I/O around it.
- **The Analyzer is the only stage that performs I/O** (loading imported `.dac` files). Lexer, Parser, and Renderers must be pure functions.

---

## Error Handling

- **No exceptions for expected errors.** Parse errors, missing references, type mismatches — all returned as `VireoResult.Err`. Never use `throw` for user-facing errors.
- **Collect all errors, do not stop at the first one.** The Analyzer especially must accumulate all errors before returning.
- **Every error must include a `SourceLocation`** (file, line, column). An error without a location is incomplete.

---

## Code Style

- **AST nodes are immutable `data class`.** Never use `var` inside an AST node.
- **Use `sealed class` / `sealed interface` for variant types** (e.g. `PropertyValue`, `Constraint`, `VireoResult`). This forces exhaustive `when` expressions — the compiler catches missing cases.
- **Use the Visitor pattern for AST traversal.** Do not write `when (node)` chains directly in renderers or analyzers — implement `AstVisitor<T>` instead.
- **No magic strings.** Token types, property keys, axis names — always enums or constants, never bare string literals.

---

## Syntax Design Rules

- **The `.dac` syntax does not exist yet — designing it is active work.** When proposing syntax, follow these constraints:
  - Component addressing must use dot notation: `file.block.component`
  - The language must support: variables, functions, classes, conditionals
  - Layout must support both explicit values (`width: 120`) and constraint expressions (`width: 50%parent`)
  - Syntax should feel familiar to developers who know any programming language
- **Do not reuse or copy an existing DSL syntax wholesale** (no YAML-style, no JSON-style, no XML). Vireo is a new language.
- **Document any proposed syntax with at least one concrete example** before implementing it.

---

## Decision Process

- **Never silently pick a TBD item.** When `ARCHITECTURE.md` marks something as `Status: undecided`, propose at least two options with tradeoffs, then ask the user to decide.
- **Record every finalized decision in the Decision Log** in `ARCHITECTURE.md` with the date and rationale.
- **Do not implement a tool or library that is not yet decided.** Proposing is fine; implementing undecided choices is not.

---

## Versioning Rules

- **Project is in Beta (`0.x`), starting at `0.1.0`.** Never bump to `1.0.0` unless the user explicitly gives the release command (e.g. "почати реліз" / "start release"). Crossing into `1.0.0` is a human decision, never inferred from a phase or checklist being complete.
- **Single source of truth:** the version lives in the root `build.gradle.kts` (`allprojects { version = "..." }`), lockstep across every module. Anything that echoes it as a literal (e.g. `vireo-cli`'s `printVersion()`) must be updated in the same change — it does not read from Gradle automatically.
- **How to pick which digit to bump** — full table and rationale in `ARCHITECTURE.md` → "Versioning Playbook". Short version while in `0.x`:
  - `0.x.PATCH` → bug fix / internal change, no behavior change for existing `.dac` files
  - `0.MINOR.x` → new capability (syntax, renderer, CLI flag) — **also used for breaking changes while in beta**, per SemVer's own pre-1.0 convention. `MAJOR` stays `0` throughout beta no matter what.
- **A version bump is its own explicit step**, never a side effect of an unrelated task — only bump when the user asks or a release is being cut.

---

## Approved External Libraries

Only these libraries may be added without asking. Any library not on this list requires explicit user approval before adding.

| Library | Version | Module | Purpose |
|---------|---------|--------|---------|
| `kotlinx-serialization-json` | 1.6.3 | `vireo-renderer-json` | Correct JSON serialization — handles all Unicode escaping, replaces manual StringBuilder |
| `clikt` | 4.2.2 | `vireo-cli` | Declarative CLI argument parsing — use when extending CLI commands (init wizard, new subcommands) |
| `okhttp` | 4.x | `vireo-renderer-figma` | HTTP client for Figma REST API — add when Phase 4 transport is decided |
| IntelliJ Platform Plugin SDK (`org.jetbrains.intellij.platform` Gradle plugin) | 2.19.0+ | `vireo-idea-plugin` (standalone project, see below) | `.dac` file type/icon, syntax highlighting, diagnostics, autocomplete inside IntelliJ IDEA — approved 2026-09-20 |

Rules for approved libraries:
- **Do not add** libraries outside this list without asking.
- **Do not add** a library to solve a problem that 5 lines of Kotlin already solve cleanly.
- When adding an approved library, record it in the Decision Log in `ARCHITECTURE.md`.

---

## What You Must Not Do

| Forbidden | Why |
|-----------|-----|
| Add a renderer that knows about another renderer | Breaks independence |
| Add `java.io.*` to `vireo-core` | Blocks future KMP migration |
| Use exceptions for parse/analysis errors | Caller can't recover cleanly |
| Add an `var` field to an AST node | AST must be immutable |
| Skip `SourceLocation` on a new AST node | Errors become unlocalizable |
| Pick a TBD tool without proposing options | User must make tooling decisions |
| Invent syntax without a concrete example | Abstract specs produce bad parsers |
