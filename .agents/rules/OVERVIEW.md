# Vireo — Project Overview

## What is Vireo?

Vireo is a **Design as a Code (DaaC)** tool. Users describe UI components in `.dac` source files using a custom programming language. The toolchain compiles these files into design outputs — currently JSON, HTML, and Figma. This makes design fully machine-readable and accessible to AI agents without any plugins or manual design tools.

**Future milestone:** reverse the pipeline — generate production UI code (with full architecture) from a Vireo design, targeting any language or framework.

---

## The `.dac` Language

`.dac` is Vireo's source format. The syntax is **custom and actively being designed** — do not reuse or propose adopting existing DSLs. When contributing to syntax design, keep the following constraints:

### Required language features
- **Component addressing** — Helm-style dot notation: `<file>.<block>.<component>`
  - Example: `forms.login.submitButton`
  - This is the primary way to reference and compose components across files
- **Logical constructs** — variables, functions, classes, conditionals — modeled after general-purpose programming languages
- **Layout model** — hybrid:
  - *Constraint system* (like Figma Auto Layout): declare relationships between elements; the engine resolves final positions
  - *Explicit values*: direct `width`, `height`, `x`, `y` when precision is needed

### Current status
Syntax is an open design problem. Contributing ideas and proposals is expected and welcome. Document decisions in `ARCHITECTURE.md`.

---

## Compilation Pipeline

```
.dac source → Parser → AST → Renderer → Output
```

| Stage | Role |
|-------|------|
| **Parser** | Reads `.dac` source, produces an Abstract Syntax Tree |
| **AST** | Renderer-agnostic intermediate representation — the stable contract between parsing and output |
| **Renderer** | Consumes AST, emits a target format |

### Current renderers
- JSON
- HTML
- Figma

### Planned renderers
- Native Vireo renderer (own format)
- Animated output (future)

Renderers are independent modules. Each targets exactly one output format. The AST is the only shared interface.

---

## User Flow

1. Run `vireo init` → CLI wizard scaffolds a new project
2. Create a `.dac` file
3. Describe UI components using the `.dac` syntax
4. Run a renderer → get JSON / HTML / Figma output

**Initialization wizard:** CLI-first. A GUI wizard (with an accompanying website and example project) is planned for later.

---

## Tech Stack

| Layer | Decision |
|-------|----------|
| Implementation language | **Kotlin** |
| Parser library | TBD — propose options, update `ARCHITECTURE.md` |
| Build tooling | TBD — propose options, update `ARCHITECTURE.md` |
| Other tools | TBD |

When a tool decision is made, record it in `ARCHITECTURE.md` with the rationale.

---

## Agent Operating Rules

- The `.dac` syntax does not exist yet — designing it is active, first-class work
- Kotlin is non-negotiable for all tooling implementation
- The AST is the stable internal interface — keep parsing and rendering cleanly separated
- For any undecided tool (parser lib, build system, etc.): propose concrete options with tradeoffs, do not silently pick one
- Update `ARCHITECTURE.md` when a decision is finalized
- Update `CONTEXT.md` at the end of every working session — what was done, what changed, what is next
- When a feature is implemented and stable, write or update the corresponding page in `DOCS/` — documentation is user-facing and must stay in sync with the implementation
