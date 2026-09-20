# Vireo — Architecture

## Platform Decision

**Pure Kotlin JVM.** No Kotlin Multiplatform (KMP).

Rationale:
- CLI wizard and render commands run fine on JVM
- Browser tooling will be written in a separate language (JS/TS) and call Vireo as a backend
- KMP adds significant complexity with no benefit at this stage
- `vireo-core` will avoid JVM-specific APIs so KMP migration remains possible later if needed

---

## Pipeline

```
Source (.dac)
     │
     ▼
  Lexer          → List<Token>
     │
     ▼
  Parser         → AST (unresolved)
     │
     ▼
  Analyzer       → AST (resolved) + errors
     │
     ▼
  Renderer       → String / structured output
```

Each stage has a single responsibility and a clean input/output type. Stages do not call each other directly — the CLI orchestrates them in sequence.

---

## Module Structure

```
vireo/
├── vireo-core/              # Shared types: AST nodes, interfaces, Result, errors
├── vireo-lexer/             # Source text → List<Token>
├── vireo-parser/            # List<Token> → AST
├── vireo-analysis/          # Resolves references, validates semantics
├── vireo-renderer-json/     # AST → JSON string
├── vireo-renderer-html/     # AST → HTML string
├── vireo-renderer-figma/    # AST → Figma API document
└── vireo-cli/               # Entry point: init wizard + render commands
```

**Dependency rule:** dependencies only flow inward — outer modules know about inner ones, never the reverse.

```
vireo-cli
  └── vireo-renderer-*
  └── vireo-analysis
        └── vireo-parser
              └── vireo-lexer
                    └── vireo-core
```

`vireo-core` has zero dependencies on other Vireo modules.

---

## Core Types (`vireo-core`)

### Result type — no exceptions for expected errors

```kotlin
sealed class VireoResult<out T> {
    data class Ok<T>(val value: T) : VireoResult<T>()
    data class Err(val errors: List<VireoError>) : VireoResult<Nothing>()
}

data class VireoError(
    val message: String,
    val location: SourceLocation
)

data class SourceLocation(val file: String, val line: Int, val column: Int)
```

All public functions in lexer, parser, analyzer, and renderers return `VireoResult<T>` — never throw.

---

### AST nodes — immutable data classes

```kotlin
data class VireoFile(
    val path: String,
    val imports: List<Import>,
    val blocks: List<Block>
)

data class Import(
    val alias: String,          // local name used in this file
    val filePath: String,       // path to the .dac file
    val location: SourceLocation
)

data class Block(
    val name: String,
    val components: List<ComponentNode>,
    val location: SourceLocation
)

data class ComponentNode(
    val name: String,
    val properties: List<Property>,
    val constraints: List<Constraint>,
    val children: List<ComponentNode>,
    val location: SourceLocation
)

// Helm-style reference: file.block.component
data class Reference(
    val file: String,
    val block: String,
    val component: String,
    val location: SourceLocation
)

// A property value is always one of three forms
sealed class PropertyValue {
    data class Literal(val value: Any, val location: SourceLocation) : PropertyValue()
    data class Ref(val reference: Reference) : PropertyValue()
    data class Expr(val source: String, val location: SourceLocation) : PropertyValue()
}

data class Property(
    val key: String,
    val value: PropertyValue,
    val location: SourceLocation
)
```

### Layout constraints — hybrid model

```kotlin
sealed class Constraint {
    // width: 120  or  height: 48
    data class Explicit(
        val axis: Axis,
        val value: Float,
        val location: SourceLocation
    ) : Constraint()

    // width: 50%parent  or  x: parent.x + 16
    data class Relational(
        val axis: Axis,
        val expression: String,
        val location: SourceLocation
    ) : Constraint()

    // layout: horizontal fill hug
    data class AutoLayout(
        val direction: Direction,
        val mainAxis: Sizing,
        val crossAxis: Sizing,
        val gap: Float = 0f,
        val location: SourceLocation
    ) : Constraint()
}

enum class Axis { WIDTH, HEIGHT, X, Y }
enum class Direction { HORIZONTAL, VERTICAL }
enum class Sizing { FILL, HUG, FIXED }
```

### Visitor pattern — for AST traversal

All renderers and the analyzer use the visitor instead of `when` chains scattered across the codebase:

```kotlin
interface AstVisitor<T> {
    fun visitFile(node: VireoFile): T
    fun visitBlock(node: Block): T
    fun visitComponent(node: ComponentNode): T
    fun visitProperty(node: Property): T
    fun visitConstraint(node: Constraint): T
}
```

---

## Lexer (`vireo-lexer`)

Input: raw `.dac` source string
Output: `VireoResult<List<Token>>`

```kotlin
data class Token(
    val type: TokenType,
    val value: String,
    val location: SourceLocation
)

enum class TokenType {
    IDENTIFIER, NUMBER, STRING,
    COLON, DOT, COMMA,
    LBRACE, RBRACE, LPAREN, RPAREN,
    KEYWORD_BLOCK, KEYWORD_COMPONENT, KEYWORD_IMPORT,
    // ... extended as syntax is defined
    EOF
}
```

The lexer is stateless — same input always produces same output.

---

## Parser (`vireo-parser`)

Input: `List<Token>`
Output: `VireoResult<VireoFile>`

Strategy: **hand-written recursive descent** (no parser generator dependency).
Switch to ANTLR4 if the grammar grows beyond what recursive descent handles cleanly.

The parser produces an unresolved AST — references like `buttons.primary.icon` are stored as `Reference` nodes but not validated yet.

---

## Analyzer (`vireo-analysis`)

Input: `VireoFile` (unresolved)
Output: `VireoResult<ResolvedFile>`

Responsibilities:
1. Load and parse imported files
2. Resolve all `Reference` nodes — verify that `file.block.component` exists
3. Detect circular imports
4. Type-check property values (e.g. `width` must be numeric)
5. Collect all errors — do not stop at the first one

The analyzer is the only stage that performs I/O (reads `.dac` files for imports).

---

## Renderer Interface (`vireo-core`)

```kotlin
interface Renderer<T> {
    fun render(file: ResolvedFile): VireoResult<T>
}
```

Renderers are:
- **Stateless** — same input, same output
- **Pure** — no file I/O, no network calls (Figma renderer returns a document object; the CLI sends it)
- **Independent** — no renderer knows about the others

---

## CLI (`vireo-cli`)

Orchestrates the pipeline:

```
vireo init                          # project wizard (CLI-first, GUI planned)
vireo render <file.dac> --to json   # compile + render to stdout or file
vireo render <file.dac> --to html
vireo render <file.dac> --to figma
vireo check <file.dac>              # parse + analyze, report errors only
```

Error output: all `VireoError` entries printed with file, line, column — same format as Kotlin compiler errors.

---

## Open Decisions

Record decisions here when made.

### Build tool

| Option | Tradeoff |
|--------|----------|
| **Gradle (Kotlin DSL)** | Standard for Kotlin multi-module, best IDE support |
| Maven | Verbose, less idiomatic for Kotlin |

> **Decision:** Gradle with Kotlin DSL (`build.gradle.kts`). **Status: decided (2026-07-29)**

### Parser strategy

| Option | Tradeoff |
|--------|----------|
| **Hand-written recursive descent** | Zero dependencies, full control over error formatting/location |
| ANTLR4 | Automated parsing, but extra build step & cryptic error handling |
| Parser combinators | Declarative in Kotlin, but performance/error formatting overhead |

> **Decision:** Hand-written recursive descent. **Status: decided (2026-07-29)**

### Expression evaluator (for `Expr` values and `Relational` constraints)

| Option | Tradeoff |
|--------|----------|
| **Custom mini-evaluator** | Minimal, fully controlled, 100% sandboxed, scoped to `.dac` needs |
| GraalVM JS | Powerful but heavy (~50MB dependency) |
| kotlinx-scripting | Native Kotlin but complex setup and high startup latency |

> **Decision:** Custom mini-evaluator — arithmetic, layout formulas, string interpolation, basic conditionals only. **Status: decided (2026-07-29)**

### Figma renderer transport

| Option | Tradeoff |
|--------|----------|
| **Figma REST API** | Full create/update, requires user auth token |
| Figma Plugin | Runs inside Figma, no auth, limited sandbox |

> **Decision:** Figma REST API transport. **Status: decided (2026-08-01)**

---

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-07-29 | Kotlin JVM — no KMP | Browser tooling in separate language; JVM sufficient for CLI |
| 2026-07-29 | AST as stable contract between stages | Decouples parser from renderers |
| 2026-07-29 | Hybrid layout: AutoLayout + Explicit + Relational | Covers both precise and constraint-based design needs |
| 2026-07-29 | Helm-style addressing: `file.block.component` | Composability across files without verbosity |
| 2026-07-29 | `VireoResult<T>` instead of exceptions | Predictable error handling; all errors surfaced to user |
| 2026-07-29 | Visitor pattern for AST traversal | Single traversal contract; renderers don't scatter `when` chains |
| 2026-07-29 | Build tool: Gradle with Kotlin DSL | Standard for Kotlin multi-module, best IDE support and type safety |
| 2026-07-29 | Parser strategy: Hand-written recursive descent | Zero dependencies, precise control over error reporting (`VireoResult`) |
| 2026-07-29 | Expression evaluator: Custom AST mini-evaluator | Lightweight, 100% sandboxed, tailored for `.dac` layout math |
| 2026-08-01 | Figma transport: Figma REST API | Pure renderer maps AST to Figma document object; CLI uses OkHttp for API sync or exports JSON |
