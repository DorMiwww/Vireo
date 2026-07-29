# Vireo — `.dac` Syntax Specification

## Status

**Syntax is actively being designed.** This document captures proposals, constraints, and finalized decisions.
When a syntax element is decided, move it from "Proposals" to the "Decision Log" at the bottom.

---

## Hard Constraints (non-negotiable)

These are fixed regardless of syntax decisions:

- Component addressing uses dot notation: `file.block.component`
- The language must support: variables, functions, classes, conditionals
- Layout supports both explicit values (`width: 120`) and constraint expressions (`width: 50%parent`)
- Syntax must feel familiar to any developer — not YAML, not JSON, not XML
- Every proposal must include at least one concrete example before implementation

---

## Core Concepts

### File structure

A `.dac` file contains:
1. **Imports** — reference other `.dac` files
2. **Blocks** — top-level named groups of components

```
imports
  ↓
blocks
  └── components
        └── properties
        └── constraints
        └── children (nested components)
```

### Component addressing

The Helm-style reference system across files:

```
buttons.primary.icon
^^^^^^^  ^^^^^^^  ^^^^
file     block    component
```

- `file` — the imported alias (not the filename directly)
- `block` — a top-level block within that file
- `component` — a component inside the block

---

## Syntax Proposals

> These are proposals — not final. Each one is marked with its status.

---

### Import syntax

**Status: proposal**

```dac
import buttons from "./buttons.dac"
import forms   from "./forms.dac"
```

- `buttons` becomes the alias used in this file
- String path resolves relative to the current file

---

### Block and component declaration

**Status: proposal**

```dac
block Login {
    component Container {
        width: 400
        height: auto
    }
}
```

- `block` — top-level named scope
- `component` — a UI element with properties and optional children

---

### Properties

**Status: proposal**

```dac
component Button {
    width: 120
    height: 40
    color: #3B82F6
    text: "Submit"
    fontSize: 14
    fontWeight: bold
    radius: 8
}
```

- Key-value pairs, one per line
- No commas, no semicolons — whitespace-delimited
- Values: numbers, strings (`"..."`), hex colors (`#...`), keywords (`bold`, `auto`, `fill`)

---

### Layout constraints

**Status: proposal**

Explicit sizing:
```dac
width: 200
height: 48
```

Auto layout (constraint system):
```dac
layout: vertical
mainAxis: fill
crossAxis: hug
gap: 16
padding: 12 24
```

Relational / percentage:
```dac
width: 50%parent
x: parent.x + 16
```

---

### Component nesting (children)

**Status: proposal**

```dac
block Card {
    component Container {
        layout: vertical
        gap: 12

        component Title {
            text: "Hello"
            fontSize: 20
        }

        component Body {
            text: "Content goes here"
            fontSize: 14
        }
    }
}
```

Children are declared inside the parent's `{}` block.

---

### Cross-file references

**Status: proposal**

```dac
import buttons from "./buttons.dac"

block LoginForm {
    component SubmitButton {
        ref: buttons.primary.default
        label: "Sign In"
    }
}
```

- `ref` links this component to a definition in another file
- Local properties (like `label`) override the referenced component's defaults

---

### Variables

**Status: proposal**

```dac
var primaryColor = #3B82F6
var spacing = 16

component Button {
    color: $primaryColor
    padding: $spacing
}
```

- `var` declares a file-scoped variable
- `$name` references a variable in a value

---

### Functions

**Status: proposal**

```dac
fun spacing(multiplier: Int): Int {
    return 8 * multiplier
}

component Card {
    padding: spacing(2)   // → 16
    gap: spacing(1)       // → 8
}
```

---

### Conditionals

**Status: proposal**

```dac
component Button {
    color: if $variant == "primary" then #3B82F6 else #6B7280
}
```

---

## Open Questions

| Question | Options | Status |
|----------|---------|--------|
| How to handle component variants? | `variant Hover { ... }` block vs. separate component | undecided |
| Inheritance / extends? | `component PrimaryButton extends Button { ... }` | undecided |
| Multiline expressions? | Parentheses or backslash continuation | undecided |
| Comment syntax? | `//` or `#` | undecided |
| Units for sizes? | Unitless (pixels implied) vs. explicit `px`, `rem` | undecided |

---

## Roadmap

| Phase | Syntax Goals |
|-------|-------------|
| **MVP** | Imports, blocks, components, explicit properties, nesting |
| **v0.2** | Cross-file `ref`, auto layout, relational constraints |
| **v0.3** | Variables, functions, basic conditionals |
| **v1.0** | Full expression language, component variants, inheritance |

---

## Decision Log

| Date | Element | Decision | Rationale |
|------|---------|----------|-----------|
| 2026-07-29 | Addressing | `file.block.component` dot notation | Helm-style, composable, readable |
| 2026-07-29 | Layout model | Hybrid: explicit + Auto Layout + relational | Covers both precise and constraint-based needs |