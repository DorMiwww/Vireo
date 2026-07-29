# Vireo — First Prompt

Copy this prompt to start a new working session on Vireo.
Replace `[TASK]` with the specific task number from the task list below.

---

## Prompt (copy → paste)

```
You are working on Vireo — a Design as a Code (DaaC) tool written in Kotlin.

Before doing anything:
1. Read agents/AGENTS.md
2. Follow every link in it and read all referenced files
3. Check agents/CONTEXT.md for current project state and open decisions

Then pick up task [TASK] from the task list in agents/START.md.

Rules:
- Kotlin only, no other JVM languages
- Follow the module dependency direction from BLOCK.md
- Never pick a TBD decision silently — propose options and ask
- Update agents/CONTEXT.md when your session ends
```

---

## Task List

Tasks are ordered by dependency — complete earlier tasks before later ones.
Each task is small enough to finish in one session and independent enough to hand to a separate agent.

### Phase 0 — Foundation (current)

| # | Task | Depends on | Output |
|---|------|-----------|--------|
| 0-A | Decide: build tool — confirm Gradle Kotlin DSL or propose alternative | — | Decision logged in ARCHITECTURE.md |
| 0-B | Decide: parser strategy — confirm recursive descent or propose alternative | — | Decision logged in ARCHITECTURE.md |
| 0-C | Decide: expression evaluator approach | — | Decision logged in ARCHITECTURE.md |
| 0-D | Scaffold Gradle multi-module project (empty modules, no logic) | 0-A | Project compiles with `./gradlew build` |

### Phase 1 — MVP

| # | Task | Depends on | Output |
|---|------|-----------|--------|
| 1-A | Implement `vireo-core` — AST node data classes, `VireoResult<T>`, `VireoError`, `SourceLocation` | 0-D | `vireo-core` module with all shared types |
| 1-B | Implement `vireo-lexer` — tokenize identifiers, numbers, strings, keywords, braces | 1-A | `Lexer` class, `Token`, `TokenType` enum |
| 1-C | Implement `vireo-lexer` tests — Example 1 and 2 from EXAMPLES.md tokenize correctly | 1-B | Passing tests |
| 1-D | Implement `vireo-parser` — parse block, component, explicit properties (no refs, no layout) | 1-B | `Parser` class producing `VireoFile` AST |
| 1-E | Implement `vireo-parser` tests — Example 1 and 2 from EXAMPLES.md parse correctly | 1-D | Passing tests |
| 1-F | Implement `vireo-renderer-json` — emit JSON from `VireoFile` AST | 1-A | `JsonRenderer : Renderer<String>` |
| 1-G | Implement `vireo-renderer-json` tests — Example 1 JSON output matches expected | 1-F, 1-D | Passing tests |
| 1-H | Implement `vireo-cli` — `vireo render <file.dac> --to json` command | 1-F, 1-D | Working CLI command |
| 1-I | End-to-end test — Example 1 and 2 from EXAMPLES.md render to correct JSON via CLI | 1-H | Passing e2e test |

### Phase 2 — Layout & Composition

| # | Task | Depends on | Output |
|---|------|-----------|--------|
| 2-A | Implement `vireo-lexer` extension — import keyword, dot notation tokens | 1-B | Extended lexer |
| 2-B | Implement `vireo-parser` extension — parse imports, `ref:` cross-file references | 2-A | Extended parser with `Import`, `Reference` nodes |
| 2-C | Implement `vireo-analysis` — load imports, resolve references, circular import detection | 2-B | `Analyzer` class, `ResolvedFile` type |
| 2-D | Implement `vireo-parser` extension — parse layout constraints (AutoLayout, Relational) | 2-A | Extended parser with `Constraint` nodes |
| 2-E | Implement `vireo-renderer-html` — emit HTML/CSS from `ResolvedFile` | 2-C | `HtmlRenderer : Renderer<String>` |
| 2-F | Implement `vireo-cli` extension — `vireo render --to html`, `vireo check` | 2-E | Extended CLI |
| 2-G | End-to-end test — Examples 3 and 4 render to JSON and HTML | 2-F | Passing e2e tests |

### Phase 3 — Language Features

| # | Task | Depends on | Output |
|---|------|-----------|--------|
| 3-A | Implement `vireo-lexer` extension — `var`, `fun`, `if`, `then`, `else`, `$` tokens | 2-A | Extended lexer |
| 3-B | Implement `vireo-parser` extension — parse `var` declarations | 3-A | `VarDeclaration` AST node |
| 3-C | Implement `vireo-parser` extension — parse `fun` declarations | 3-A | `FunDeclaration` AST node |
| 3-D | Implement `vireo-parser` extension — parse conditionals (`if ... then ... else`) | 3-A | `ConditionalExpr` AST node |
| 3-E | Implement expression evaluator — arithmetic, string interpolation, basic conditionals | 3-B, 3-C, 3-D | `ExprEvaluator` in `vireo-analysis` |
| 3-F | End-to-end test — Examples 5 and 6 work correctly | 3-E | Passing e2e tests |

---

## How to Use This File

1. Pick the lowest-numbered incomplete task
2. Paste the prompt above into a new Claude session, replacing `[TASK]` with the task number
3. Each task is designed to be completable independently — the agent reads CONTEXT.md to understand current state
4. When a task is done, update CONTEXT.md and check off the item in ROADMAP.md