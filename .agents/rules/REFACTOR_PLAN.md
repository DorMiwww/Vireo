# Vireo — Internal Refactor Plan

**Status:** `[not started]`
**Type:** Temporary working plan — not part of the AGENTS.md router. Linked from `CONTEXT.md → What Is Next` only.
**Scope:** Internal code organization only. No syntax changes, no new features, no behavior changes. Every phase must be a no-op from the outside: same CLI output, byte-for-byte, before and after.

This plan splits large, monolithic single-file modules into smaller files organized by responsibility, in canonical per-module folders, and cleans up duplicated logic — without changing what the compiler, renderers, or CLI actually produce. It does **not** touch `.dac` syntax (see `SYNTAX.md`) or add features (see `ROADMAP.md`).

---

## Ground Rules (apply to every phase, no exceptions)

1. **Test-first, not TDD-red-green.** These are *characterization* refactors, not new behavior. The rule is:
   > Test passes on the untouched code → refactor → the same test passes, unchanged, on the refactored code.
   If a test is red *before* you've changed anything, the test (or your understanding of current behavior) is wrong — fix that first. Never "refactor to make a red test pass" under this plan.
2. **Golden output over fragment assertions.** Several modules (`vireo-renderer-html`, `vireo-renderer-figma`, `vireo-analysis`) have thin test coverage relative to their size and assert on fragments, not whole output. Before restructuring such a module, add golden/characterization tests that pin the *entire* output for real fixtures. This is often the majority of the work in a phase — budget for it.
3. **One phase = one module = one commit (or a tight series of commits ending in a green build), ending with this file updated.** Do not start phase N+1 in the same session unless phase N is committed and marked complete below.
4. **Never mix a dedup/move with a library or behavior change.** Adopting `kotlinx-serialization-json` more fully in `vireo-renderer-json`, or any other approved-library change, is a separate proposal — never bundled into a file-split phase, because it changes output and breaks golden tests for the wrong reason.
5. **Respect `BLOCK.md` at all times:** module dependency direction (`vireo-cli → renderer-* → vireo-analysis → vireo-parser → vireo-lexer → vireo-core`) is never reversed; `vireo-core` gets no `java.io`/`java.net`; AST nodes stay immutable `data class`; no new TBD library picks without asking.
6. **Every phase ends green:** `./gradlew test` (full suite) passes, not just the touched module's tests.
7. **Mark the phase complete in this file** (flip the status tag, check every box in that phase's Definition of Done, note the commit hash) before moving on or ending the session. If a phase is only partially done, leave it `[in progress]` and write down exactly what remains under "Handoff notes" for that phase — the next session/chat must be able to resume from this file alone.

---

## How to resume this plan in a new chat

```
You are working on Vireo. Read .agents/rules/AGENTS.md and everything it links to,
then read .agents/rules/REFACTOR_PLAN.md in full.

Pick up the first phase in REFACTOR_PLAN.md that is not marked [complete].
Follow that phase's steps exactly: baseline test run → add/expand tests →
run tests → refactor → run tests again → update this plan file → commit.

Do not start a second phase in this session unless the first is committed
and marked [complete]. Do not touch .dac syntax or add features.
```

---

## Phase Order & Why

Ordered by ascending blast radius, not by module list order in `settings.gradle.kts`:

`core → lexer → renderer-json → renderer-html → renderer-figma → analysis → cli → parser`

- `vireo-core` first: pure type/file moves, zero logic changes, and every other module depends on it, so a passing full-suite run here is the cheapest possible safety signal.
- `parser` last: it's the one genuinely hard case. `Parser.kt` is a single `object` wrapping one `private class FileParser` (~560 lines) that holds all recursive-descent parsing as members of one class. Kotlin has no partial classes, so this split is **composition, not file-moving** — higher risk, done last with the most practice behind it.

---

## Phase 0 — Baseline & Safety Net `[not started]`

**Applies to:** whole repo. Must be done once, before Phase 1.

### Steps
1. `git status` — working tree must be clean before phase work starts. If not, stop and ask the user whether to commit or stash first; do not decide this unilaterally.
2. Run the full suite and record the result verbatim in this file's "Baseline log" below:
   ```
   ./gradlew test
   ```
3. Confirm `./gradlew build` also succeeds (compiles every module, not just tests).
4. Note the current commit hash as the refactor's starting point.

### Definition of Done
- [ ] Working tree clean at phase start
- [ ] `./gradlew test` → BUILD SUCCESSFUL, logged below
- [ ] `./gradlew build` → BUILD SUCCESSFUL
- [ ] Starting commit hash recorded

### Baseline log
```
Recorded 2026-09-20: ./gradlew test → BUILD SUCCESSFUL (39 actionable tasks: 8 executed, 31 up-to-date)
Working tree: clean, HEAD = ca99e39
```

---

## Phase 1 — `vireo-core` `[not started]`

**Risk:** lowest. **Current size:** `Ast.kt` 131 lines, `AstVisitor.kt` 11 lines, `Renderer.kt` 5 lines, `VireoResult.kt` 17 lines — 164 lines total, already fairly small, but `Ast.kt` mixes five unrelated concerns in one file: file/block/component nodes, the `Reference`/`Import` addressing types, `PropertyValue`, layout `Constraint`s + `Axis`/`Direction`/`Sizing` enums, and `ResolvedFile`.

### Target layout
```
vireo-core/src/main/kotlin/com/vireo/core/
  ast/
    VireoFile.kt        # VireoFile, Import, Block, ComponentNode, ResolvedFile
    Reference.kt         # Reference (file.block.component addressing)
    Property.kt          # Property, PropertyValue (sealed)
    Constraint.kt         # Constraint (sealed), Axis, Direction, Sizing
    Declarations.kt       # VarDeclaration, FunDeclaration, Parameter (if present in Ast.kt)
  AstVisitor.kt           # unchanged, stays at package root
  Renderer.kt             # unchanged
  VireoResult.kt          # unchanged (VireoResult, VireoError, SourceLocation — or split SourceLocation out if it grows)
```
Read `Ast.kt` first and confirm the exact set of declarations before splitting — the list above is a best-grounded estimate, not a transcription. Keep every type as an immutable `data class` / `sealed class` exactly as-is; this phase moves code, it does not change any type's shape.

### Test-first steps
1. Baseline: `./gradlew :vireo-core:test` — must be green (it already has `AstTest.kt`, 106 lines — check it actually exercises every AST node, not just a subset; add coverage for any node left untested before moving it).
2. Move declarations file-by-file (one `git mv`-free `Write` + delete per new file, not a bulk cut). After each individual move: `./gradlew :vireo-core:test` and `./gradlew build` (build, because every module depends on core — a broken import anywhere surfaces here).
3. When all moves are done: full suite `./gradlew test`.

### Definition of Done
- [ ] All `vireo-core` tests green before first move
- [ ] Each moved file compiles (`./gradlew build`) immediately after its own move — no batching multiple type-moves before checking
- [ ] Full suite green after last move
- [ ] No `java.io`/`java.net` introduced (grep confirms none — this was already true before the phase)
- [ ] Commit hash recorded below

### Handoff notes
_(fill in if left `[in progress]`)_

---

## Phase 2 — `vireo-lexer` `[not started]`

**Risk:** low. **Current size:** `Lexer.kt` 232 lines (one `object Lexer` with a single public `tokenize()` and presumably private scanning helpers), `Token.kt` 52 lines. Test coverage is already strong relative to size (422 test lines vs 284 main) — this phase is mostly organizational, not test-writing.

### Target layout
```
vireo-lexer/src/main/kotlin/com/vireo/lexer/
  Token.kt              # unchanged
  Lexer.kt              # public tokenize() entry point only, delegates to:
  internal/
    Scanner.kt           # the character-by-character scan loop + keyword/number/string recognition helpers
```
First read `Lexer.kt` fully and list its private members before deciding the exact split — if the whole file is already one cohesive scan loop under ~230 lines, a split may not be justified. **If, after reading it, there is no real internal seam, leave it as one file and mark this phase done with a one-line note why — do not force a split for its own sake.**

### Test-first steps
1. Baseline: `./gradlew :vireo-lexer:test` — must be green.
2. If splitting: move the scan-loop internals into `internal/Scanner.kt`, keep `Lexer.tokenize()` as the thin public entry point. Run `./gradlew :vireo-lexer:test` after the move.
3. Full suite `./gradlew test`.

### Definition of Done
- [ ] Baseline green
- [ ] Split done (or explicitly declined with reason recorded here)
- [ ] Full suite green after
- [ ] Commit hash recorded below

### Handoff notes

---

## Phase 3 — `vireo-renderer-json` `[not started]`

**Risk:** low. **Current size:** 104 main lines, 181 test lines — already the best-tested module relative to size, and small enough it may not need splitting at all. Treat this phase as a dry run for the golden-test technique before the harder HTML/Figma phases.

### Steps
1. Baseline: `./gradlew :vireo-renderer-json:test`.
2. Read `JsonRenderer.kt`. If it's one small cohesive `object`, no file split is needed — confirm and record that decision here rather than inventing structure.
3. Add one golden test if none exists: render `designs/login.dac` (already has resolved output implicitly exercised elsewhere) through `JsonRenderer` and assert against a checked-in `src/test/resources/golden/login.json` — this becomes the template for Phases 4–5.
4. Full suite `./gradlew test`.

### Definition of Done
- [ ] Baseline green
- [ ] Golden test added (or existing coverage confirmed sufficient, with reasoning noted)
- [ ] Full suite green after
- [ ] Commit hash recorded below

### Handoff notes

---

## Phase 4 — `vireo-renderer-html` `[not started]`

**Risk:** medium-high. **Current size:** 424 main lines vs only 156 test lines — thin relative to size, and `renderComponent()` alone spans roughly lines 40–281 (~240 lines) in one function. This is the first phase where **writing the safety net is the bulk of the work**, not moving code.

### Current state (grounded)
`HtmlRenderer` is one `object : Renderer<String>` with `render(file: ResolvedFile)` delegating to an overload `render(file: VireoFile, loadedFiles: Map<String, VireoFile>)`, plus: `renderBlock`, the giant `renderComponent`, `resolveComponentRef`, `findComponent`, `resolvePath`, `extractPropertyValueString`, `formatRelationalCss`, a `Float.toIntIfWhole()` extension, and `escapeHtml`. It contains 16 `when (...)` blocks written inline — `BLOCK.md` requires `AstVisitor<T>` for AST traversal in renderers instead of scattered `when` chains. **Do not fix that in this phase** — it's a real gap but a behavior-preserving redesign of how traversal works, not a file-move; it's tracked under Deferred Proposals below and needs an explicit go-ahead first.

`findComponent`, `resolvePath`, and `extractPropertyValueString` are duplicated near-verbatim in `vireo-renderer-figma/FigmaRenderer.kt`. Confirmed: neither uses `java.io`/`java.net`, both renderers already depend on `vireo-core`, so moving the identical ones there does **not** violate the "renderer must not know about another renderer" rule and is not a TBD library pick — it's a legal target under the existing dependency graph. **Do the extraction in Phase 5** (after Figma's own golden tests exist too), so both call sites have a safety net before the move. In this phase, only note (or re-confirm) that these three are byte-identical between the two files; if they've drifted, keep them separate and record why instead of forcing a shared version.

### Target layout
```
vireo-renderer-html/src/main/kotlin/com/vireo/renderer/html/
  HtmlRenderer.kt         # render() entry points + renderBlock() only
  ComponentRenderer.kt     # renderComponent() broken into named steps (box model, text, layout/flex CSS, colors/border/radius) — same output, decomposed into private functions, not a new abstraction
  ReferenceResolution.kt   # resolveComponentRef, findComponent, resolvePath  (candidate to move to vireo-core in Phase 5 once confirmed identical to Figma's copy)
  CssFormat.kt             # formatRelationalCss, Float.toIntIfWhole(), escapeHtml, extractPropertyValueString
```

### Test-first steps
1. Baseline: `./gradlew :vireo-renderer-html:test` — green.
2. **Add golden tests before any code move.** The repo already has real fixtures with checked-in expected output: `designs/login.dac` → `designs/login.html`, `showcase/card.dac` → `showcase/card.html`, `showcase/pricing.dac` → `showcase/pricing.html`, `showcase/dashboard.dac` → `showcase/dashboard.html`. Verify each checked-in `.html` is actually current (render fresh via CLI and diff against the checked-in file — if they differ, stop and ask the user which is authoritative before using it as a golden baseline). Add a JUnit test per fixture that renders the `.dac` through `HtmlRenderer` and asserts equality against the checked-in `.html` byte-for-byte.
3. Run the new golden tests on **unmodified** code — they must pass as-is. If any fails, the fixture or the renderer has an existing bug outside this plan's scope; stop and flag it rather than "fixing" it mid-refactor.
4. Only once golden tests are green: split `renderComponent()` into named private functions with identical logic (no behavior change — copy-paste-and-name, not rewrite). Move files per the target layout.
5. Re-run unit tests, then golden tests, after every file move.
6. Full suite `./gradlew test`.

### Definition of Done
- [ ] Baseline green
- [ ] Golden tests added for login/card/pricing/dashboard HTML output, confirmed green on unmodified code
- [ ] `renderComponent()` decomposed, output unchanged (golden tests still green)
- [ ] Files moved into target layout, full suite green
- [ ] Duplication with `FigmaRenderer` confirmed identical (or documented as diverged) — not yet extracted
- [ ] Commit hash recorded below

### Handoff notes

---

## Phase 5 — `vireo-renderer-figma` `[not started]`

**Risk:** medium-high. **Current size:** 566 main lines (459 in `FigmaRenderer.kt` + 107 in `FigmaDocument.kt`) vs only 147 test lines. `renderComponent()` spans roughly lines 59–357 (~300 lines) — the largest single function in the codebase.

### Target layout
```
vireo-renderer-figma/src/main/kotlin/com/vireo/renderer/figma/
  FigmaDocument.kt         # unchanged (data types)
  FigmaRenderer.kt          # render() entry points + renderBlock() only
  ComponentRenderer.kt      # renderComponent() decomposed (frame/text/rectangle node construction, fills, strokes, auto-layout params) — same split shape as HTML's Phase 4
  CssFormat.kt / FigmaFormat.kt   # extractPropertyValueString and any Figma-specific formatting left after dedup
```

### Test-first steps
1. Baseline: `./gradlew :vireo-renderer-figma:test`.
2. Same golden-test technique as Phase 4, using `designs/login.dac` → `designs/login.figma.json`, `showcase/{card,pricing,dashboard}.dac` → their checked-in `.figma.json`. Verify freshness the same way (render, diff against checked-in, ask before trusting a stale one) before using as golden baseline. **Note:** git history shows `designs/login.figma.json` and both renderers were modified together in a recent uncommitted-then-committed change (`6227c12`) — re-render explicitly rather than assuming the checked-in JSON matches the current code.
3. Golden tests must pass unmodified before any move.
4. Decompose `renderComponent()`, same discipline as Phase 4: copy-paste-and-name, no logic changes, golden tests stay green throughout.
5. **Now do the cross-renderer dedup deferred from Phase 4:** if `findComponent`, `resolvePath`, and `extractPropertyValueString` are confirmed identical between `HtmlRenderer` and `FigmaRenderer`, move them to `vireo-core` (e.g. `vireo-core/.../ast/ReferenceResolution.kt`) as plain functions operating only on `vireo-core` AST types — no Figma/HTML-specific types in the signature. Update both renderers to call the shared version. Run both modules' full test suites (unit + golden) after.
6. Full suite `./gradlew test`.

### Definition of Done
- [ ] Baseline green
- [ ] Golden tests added for login/card/pricing/dashboard Figma JSON output, confirmed green on unmodified code (checked-in JSON re-verified fresh, not assumed)
- [ ] `renderComponent()` decomposed, golden tests still green
- [ ] Shared pure helpers (`findComponent`/`resolvePath`/`extractPropertyValueString`) moved to `vireo-core` if identical; both renderers updated; full suite green — or, if they'd diverged, left separate with the reason recorded here
- [ ] Commit hash recorded below

### Handoff notes

---

## Phase 6 — `vireo-analysis` `[not started]`

**Risk:** medium. **Current size:** `Analyzer.kt` 233 lines (reasonably organized, already uses `AstVisitor` via a private `ReferenceAndPropertyVisitor`), `ExprEvaluator.kt` 656 lines — the single largest file in the codebase, one `object ExprEvaluator` wrapping a top-level `internal sealed class EvalValue`, a top-level `private enum class TokenType` / `private data class ExprToken` (~line 243), and one `private class ExprEvaluatorEngine` that holds two genuinely separate concerns as members of the same class:
- **AST walking** (`evaluateFile`, `evaluateBlock`, `evaluateComponent`, `evaluatePropertyValue`, `interpolateString`, `evaluateExpressionString`) — walks `VireoFile`/`Block`/`ComponentNode`, holds `varScope`/`funScope`.
- **Expression tokenizing + recursive-descent parsing** (`tokenize`, `parse`, `parseExpression`, `parseEquality`, `compareValues`, `parseAdditive`, `parseMultiplicative`, `parsePrimary`, `interpolateStringInParser`, `match`, `check`, `advance`, `isAtEnd`, `peek`, `previous`) — operates only on `List<ExprToken>` / raw strings, matches the "custom mini-evaluator" decision already logged in `ARCHITECTURE.md`.

Kotlin has no partial classes — splitting `ExprEvaluatorEngine` across files means **composition**, same as the Parser problem, just smaller in scope: extract the tokenizer+parser members into their own class that `ExprEvaluatorEngine` calls, not a mechanical file-move.

### Target layout
```
vireo-analysis/src/main/kotlin/com/vireo/analysis/
  Analyzer.kt              # unchanged
  expr/
    EvalValue.kt            # the sealed EvalValue + toPropertyValue/toLiteralValue/toDisplayString (top-level today, just move + internal visibility)
    ExprToken.kt             # TokenType enum + ExprToken data class (currently `private` — becomes `internal` to be visible across files)
    ExpressionEngine.kt      # tokenize() + parse()/parseExpression()/.../previous() as one cohesive class — pure, stateless per call, takes the raw string + scopes it needs as parameters
  ExprEvaluator.kt           # object ExprEvaluator (public entry) + ExprEvaluatorEngine (AST walking only), delegates raw-expression evaluation to ExpressionEngine
```

### Test-first steps
1. Baseline: `./gradlew :vireo-analysis:test` — 431 test lines vs 889 main (233+656) — check `ExprEvaluatorTest.kt` (172 lines) actually covers the tokenizer/parser edge cases (negative numbers, string interpolation with `$var` inside literals, `==` comparisons, nested arithmetic) before restructuring around them — add cases for any gap found.
2. Extract `EvalValue` and `ExprToken`/`TokenType` to their own files first (mechanical, visibility-only change: `private` → `internal`). Test after each.
3. Extract the tokenizer+parser block into `ExpressionEngine`, keeping method bodies unchanged — only the class boundary and how `ExprEvaluatorEngine` invokes it changes (a method call instead of `this.tokenize()`/`this.parse()`).
4. Test after the extraction; then full suite.

### Definition of Done
- [ ] Baseline green, gaps in `ExprEvaluatorTest.kt` filled if found
- [ ] `EvalValue` and `ExprToken`/`TokenType` moved to `expr/`, tests green
- [ ] `ExpressionEngine` extracted via composition, `ExprEvaluatorEngine` delegates to it, tests green
- [ ] Full suite green
- [ ] Commit hash recorded below

### Handoff notes

---

## Phase 7 — `vireo-cli` `[not started]`

**Risk:** medium (user-facing entry point, but well-tested: 709 test lines vs 298 main). **Current size:** `Main.kt` 261 lines with `main`, `executeCli`, `printUsage`, `handleRender` (60–210, ~150 lines covering json/html/figma dispatch), `handleCheck`, `printErrors`; `FigmaApiTransport.kt` 37 lines already separate.

### Target layout
```
vireo-cli/src/main/kotlin/com/vireo/cli/
  Main.kt              # main() + executeCli() dispatch only
  RenderCommand.kt      # handleRender() split by target: dispatch stays here, or split further into renderToJson/Html/Figma helpers if handleRender's internal branching is large
  CheckCommand.kt        # handleCheck()
  CliOutput.kt           # printUsage, printErrors
  FigmaApiTransport.kt   # unchanged
```
Read `handleRender` fully first — if its three format branches are short and mostly dispatch (calling into the renderer modules, which already do the real work), a full command-per-file split may be more ceremony than value; in that case keep `handleRender` as one function but still extract `printUsage`/`printErrors` into `CliOutput.kt`, and record that decision here.

### Test-first steps
1. Baseline: `./gradlew :vireo-cli:test` — includes `EndToEndTest.kt` (520 lines) exercising real CLI invocations end-to-end; this is already a strong safety net, likely no new tests needed for this phase.
2. Split per target layout (or the reduced version decided above).
3. Test after each file extraction; full suite at the end.

### Definition of Done
- [ ] Baseline green
- [ ] Split done per the layout above, or reduced split with reasoning recorded
- [ ] Full suite green after
- [ ] Commit hash recorded below

### Handoff notes

---

## Phase 8 — `vireo-parser` `[not started]`

**Risk:** highest — do this last, after the technique has been exercised seven times. **Current size:** `Parser.kt` 584 lines: `object Parser` (2 public `parse()` overloads) wrapping one `private class FileParser(tokens, filePath)` that holds **all** recursive-descent parsing as members: imports, `var`/`fun` declarations, blocks, components, properties, constraints (explicit/auto-layout/relational), conditionals, and the token-cursor primitives (`match`/`check`/`advance`/etc.), all sharing one mutable `current` cursor and `errors` list.

This is the same "no partial classes" problem as Phase 6, at full scale. **Composition, not file-splitting**, and it must not change the mutable-cursor contract that every sub-parser depends on.

### Target layout (composition, not file-move)
```
vireo-parser/src/main/kotlin/com/vireo/parser/
  Parser.kt                # object Parser — public parse() entry points only
  internal/
    TokenCursor.kt          # current-position cursor + match/check/advance/isAtEnd/peek/previous — the shared primitive every sub-parser needs
    FileParser.kt            # top-level: imports, var/fun declarations, block dispatch — owns a TokenCursor, delegates to:
    ComponentParser.kt        # component + property parsing
    ConstraintParser.kt       # explicit/auto-layout/relational constraint parsing
    ExpressionParser.kt       # conditional / expression-value parsing (if distinct from ConstraintParser — read the file first to confirm the real seam)
```
Before writing any code: read `Parser.kt` top to bottom and map every method to which grammar concern it belongs to, and how much shared mutable state (`current`, `errors`) each one touches. If the coupling through `current` is too tight to cleanly separate without passing the cursor object around everywhere, that's a legitimate reason to do a lighter split (e.g. just pull constraint-parsing out, leave the rest) — record the actual decision here, don't force the four-file layout above if the grammar doesn't support it cleanly.

### Test-first steps
1. Baseline: `./gradlew :vireo-parser:test` — 586 test lines vs 584 main, already close to 1:1 coverage; check it covers every branch in the target files above (imports, each constraint kind, conditionals) before relying on it as the safety net.
2. Introduce `TokenCursor` first as an extracted-but-still-used-only-by-`FileParser` class (no behavior change, pure indirection) — smallest possible first step, test immediately.
3. Move one grammar concern out at a time (e.g. constraints first — self-contained, lower risk — then components, then declarations), testing after each single move, never batching.
4. Full suite after every extraction, not just at the end — this phase has the highest chance of a subtle regression (operator precedence, error recovery ordering) that only shows up as a spurious parse error somewhere unrelated.

### Definition of Done
- [ ] Baseline green, coverage gaps (if any) filled first
- [ ] `TokenCursor` extracted, tests green
- [ ] Each grammar concern moved out one at a time, full suite green after each
- [ ] Final layout matches plan or documented deviation recorded
- [ ] Commit hash recorded below

### Handoff notes

---

## Deferred Proposals — need an explicit user decision, not scheduled in any phase above

These are real gaps found while grounding this plan. They are **redesigns**, not refactors — moving code around doesn't fix them, and per `BLOCK.md`'s decision process they need to be posed as options, not picked silently. Do not fold either into a phase above without the user signing off first.

### 1. Renderers use raw `when (node)` chains instead of `AstVisitor<T>`
`BLOCK.md`: *"Use the Visitor pattern for AST traversal. Do not write `when (node)` chains directly in renderers or analyzers — implement `AstVisitor<T>` instead."*
Confirmed: `FigmaRenderer.kt` has 17 inline `when (...)` blocks, `HtmlRenderer.kt` has 16. `Analyzer.kt` already does this correctly via `ReferenceAndPropertyVisitor : AstVisitor<Unit>`. The renderers do not.
**Options to pose to the user when this is picked up:**
- (a) Convert both renderers to implement `AstVisitor<T>` for real, matching `Analyzer.kt`'s pattern — larger change, brings the codebase into `BLOCK.md` compliance.
- (b) Leave as-is and amend `BLOCK.md` to scope the visitor requirement to the analyzer only — smaller change, but weakens a rule that was written deliberately.
This plan does not choose between them.

### 2. `vireo-core` → `vireo-analysis` dependency for cross-renderer helper types
Not yet a problem, but if the Phase 5 dedup of `findComponent`/`resolvePath`/`extractPropertyValueString` into `vireo-core` turns out to need anything from `vireo-analysis` (e.g. `ResolvedFile`-aware helpers instead of raw `VireoFile`), that would need `vireo-core` depending on `vireo-analysis` — which is backwards per the dependency graph and not allowed. If Phase 5 hits this, stop and bring it back as a proposal rather than resolving it in-phase.

---

## Phase Completion Log

| Phase | Module | Status | Commit(s) | Date |
|-------|--------|--------|-----------|------|
| 0 | — (baseline) | not started | — | — |
| 1 | vireo-core | not started | — | — |
| 2 | vireo-lexer | not started | — | — |
| 3 | vireo-renderer-json | not started | — | — |
| 4 | vireo-renderer-html | not started | — | — |
| 5 | vireo-renderer-figma | not started | — | — |
| 6 | vireo-analysis | not started | — | — |
| 7 | vireo-cli | not started | — | — |
| 8 | vireo-parser | not started | — | — |
