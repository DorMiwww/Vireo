# Vireo — Glossary

Canonical definitions for all terms used in this project.
When in doubt about a term, check here first. Update when new terms are introduced.

---

## Core Terms

### `.dac` file
The source file format for Vireo. Users write UI descriptions in `.dac` files using the Vireo language. Extension stands for **Design As Code**.

### Block
A top-level named group inside a `.dac` file. Blocks are the primary unit of organization. A file can contain multiple blocks.
```dac
block Login { ... }
block Signup { ... }
```

### Component
A single UI element declared inside a Block. Components have properties, constraints, and optionally children (nested components).
```dac
component Button { ... }
```

### Property
A key-value pair on a Component that describes its visual appearance (`color`, `fontSize`, `text`, etc.).
```dac
color: #3B82F6
fontSize: 14
```

### Constraint
A layout rule on a Component. Can be explicit (`width: 120`), relational (`width: 50%parent`), or auto layout (`layout: vertical`).

### Reference
A Helm-style pointer to a component in another file: `file.block.component`.
- `file` — the import alias
- `block` — block name inside that file
- `component` — component name inside that block

Example: `buttons.Primary.Default`

### Import
A declaration at the top of a `.dac` file that pulls in another `.dac` file under an alias.
```dac
import buttons from "./buttons.dac"
```

---

## Pipeline Terms

### Lexer (`vireo-lexer`)
The first pipeline stage. Takes raw `.dac` source text and produces a flat list of **Tokens**. Has no knowledge of grammar — only recognizes characters and keywords.

### Token
The atomic unit output by the Lexer. Has a type (e.g. `IDENTIFIER`, `NUMBER`, `KEYWORD_BLOCK`) and a source location.

### Parser (`vireo-parser`)
The second pipeline stage. Takes a `List<Token>` and produces an **AST**. Understands the grammar — how tokens combine into blocks, components, and properties.

### AST (Abstract Syntax Tree)
The intermediate representation of a `.dac` file after parsing. It is **renderer-agnostic** — the stable contract between the Parser and all Renderers. Defined in `vireo-core`.

### Unresolved AST
The AST as produced by the Parser. References (`file.block.component`) are stored but not validated — the target may or may not exist.

### Analyzer (`vireo-analysis`)
The third pipeline stage. Takes an Unresolved AST and produces a **Resolved AST**. Validates all references, detects circular imports, and type-checks properties.

### Resolved AST / `ResolvedFile`
The AST after the Analyzer has validated all references. Safe to pass to a Renderer.

### Renderer
The final pipeline stage. Takes a `ResolvedFile` and emits an output (JSON string, HTML string, Figma document). Each renderer is a separate module. Renderers are **pure** and **stateless**.

---

## Type Terms

### `VireoResult<T>`
The return type of every public function in the pipeline. Either `Ok(value)` or `Err(errors)`. No exceptions are used for expected errors.

### `VireoError`
A single error with a human-readable `message` and a `SourceLocation`.

### `SourceLocation`
Points to a specific place in a `.dac` file: `file`, `line`, `column`. Every AST node and every `VireoError` carries one.

### `AstVisitor<T>`
The interface used by Renderers and the Analyzer to traverse AST nodes without scattering `when` chains across the codebase.

---

## Layout Terms

### Explicit sizing
Setting a dimension directly: `width: 200`, `height: 48`. Pixel values.

### Auto Layout
Constraint-based layout where children are arranged by direction and sizing rules — like Figma Auto Layout or CSS Flexbox.
```dac
layout: vertical
mainAxis: fill
crossAxis: hug
gap: 16
```

### Relational constraint
A size or position expressed relative to the parent: `width: 50%parent`, `x: parent.x + 16`.

### Sizing modes
- `fill` — expand to fill available space
- `hug` — shrink to fit children
- `fixed` — use an explicit pixel value

---

## Module Terms

### `vireo-core`
The innermost module. Contains AST node definitions, `VireoResult`, `VireoError`, `SourceLocation`, `Renderer` interface, and `AstVisitor`. Zero dependencies on other Vireo modules.

### `vireo-cli`
The outermost module. Entry point for all user-facing commands (`vireo render`, `vireo init`, `vireo check`). Orchestrates the full pipeline.

---

## Project Terms

### DaaC (Design as a Code)
The paradigm Vireo implements. Design is described in source files, version-controlled, and compiled — the same way code is.

### Phase
A development milestone in the Roadmap. Each phase has a clear deliverable. See `ROADMAP.md`.