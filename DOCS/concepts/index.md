# Core Concepts

Vireo introduces **Design as a Code (DaaC)**: treating design tokens, component hierarchies, and interface layouts as structured code rather than static artboards.

---

## The Compilation Pipeline

Vireo operates like a programming language compiler:

```
.dac Source Code
      │
      ▼
┌──────────────┐
│    Lexer     │  Tokenizes identifiers, numbers, colors, keywords, braces
└──────┬───────┘
      │
      ▼
┌──────────────┐
│    Parser    │  Builds Abstract Syntax Tree (AST)
└──────┬───────┘
      │
      ▼
┌──────────────┐
│   Analyzer   │  Resolves imports, validates references, evaluates expressions
└──────┬───────┘
      │
      ▼
┌──────────────┐
│  Renderers   │  Target-specific code emission (HTML, JSON, Figma)
└──────────────┘
```

The **AST** (`ResolvedFile`) serves as the universal contract between the language parsing engine and all output targets. Any renderer can transform the AST into UI formats without knowledge of the source text.

---

## File Structure

A `.dac` file is organized into four main layers:

1. **Imports:** Link external `.dac` files with custom aliases.
2. **Declarations:** File-scoped variables (`var`) and functions (`fun`).
3. **Blocks:** Named, top-level containers that group related UI components.
4. **Components:** Declared UI elements with properties, constraints, and children.

```dac
// 1. Imports
import buttons from "./components/buttons.dac"

// 2. Global tokens / variables
var brandColor = #2563EB

// 3. Block scope
block Authentication {

    // 4. Component definition
    component LoginForm {
        width: 360
        layout: vertical
        gap: 16

        component Submit {
            ref: buttons.Primary.Default
            color: $brandColor
        }
    }
}
```

---

## Key Principles

- **No Ambiguity:** Properties are strictly typed and deterministic. Whitespace, layout directions, and hierarchy are explicit.
- **Composable Inheritance:** Components inherit styles using `ref:` and selectively override specific properties.
- **Multi-Renderer Output:** The same `.dac` file compiles to interactive web previews, machine-readable JSON for agents, or vector layers in Figma.

---

## Read Next
- [Addressing & Composition](./addressing.md) — How cross-file imports and `ref:` inheritance work.
- [Expressions & Variables](./expressions.md) — Dynamic values, variables, and conditionals.
- [Component Model](../components/index.md) — How components and properties are defined.
