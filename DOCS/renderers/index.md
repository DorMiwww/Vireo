# Renderers Overview

In Vireo, a **Renderer** is a pluggable module that consumes a resolved AST (`ResolvedFile`) and produces an output artifact for human review, toolchain integration, or design canvases.

---

## Supported Renderers

Vireo currently ships with three production renderers:

| Renderer Target | Command Flag | Output Artifact | Primary Use Case |
| :--- | :--- | :--- | :--- |
| **HTML** | `-o html` | Standalone HTML or HTML snippet | Interactive web preview, browser testing, design review |
| **JSON AST** | `-o json` | Normalized JSON schema tree | AI agent analysis, programmatic linting, toolchain pipelines |
| **Figma** | `-o figma` | Figma AST (`*.figma.json`) | Canvas layer generation via the Vireo Figma Desktop plugin |

---

## Architecture: Renderer Decoupling

Renderers are strictly decoupled from the syntax parser and semantic analyzer:

```
.dac Source ──► Parser ──► AST (ResolvedFile)
                              ├──► HtmlRenderer ──► *.html
                              ├──► JsonRenderer ──► *.json
                              └──► FigmaRenderer ──► *.figma.json
```

- **Renderer Independence:** Renderers never read raw source code or tokenize characters. They only traverse strongly-typed Kotlin AST data classes.
- **Uniform Semantics:** Layout constraints, colors, and font definitions are evaluated identically across all targets.

---

## Detailed Renderer Guides

- [HTML Renderer](./html.md) — Options for standalone pages, themes, and snippets.
- [JSON AST Renderer](./json.md) — Machine-readable AST format for tooling and AI.
- [Figma Renderer & Plugin](./figma.md) — Two-step compilation and desktop plugin guide.
