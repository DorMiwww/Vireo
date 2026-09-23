# Vireo Documentation

Welcome to the official documentation for **Vireo** — a modern **Design as a Code (DaaC)** platform and compiler.

Vireo replaces manual design files with human-readable `.dac` source code. It treats user interface design with the same rigor as software engineering: component modularity, inheritance, layout constraint solving, and automated compilation into production targets including **HTML/CSS**, **JSON AST**, and **Figma**.

---

## Documentation Overview

Explore the core sections of the Vireo platform:

| Section | Description |
| :--- | :--- |
| [Getting Started](./getting-started.md) | Install Vireo CLI, initialize your workspace, and render your first design |
| [Core Concepts](./concepts/index.md) | The DaaC philosophy, file structure, and compiler architecture |
| [Addressing & Composition](./concepts/addressing.md) | Cross-file references with dot-notation (`file.block.component`) and `ref:` overrides |
| [Expressions & Variables](./concepts/expressions.md) | Dynamic design tokens with `var`, helper `fun`, and conditionals |
| [Component Model](./components/index.md) | Structure of `.dac` components, nesting, and lifecycle |
| [Properties Catalogue](./components/properties.md) | Exhaustive reference of sizing, typography, colors, borders, and shadows |
| [Buttons & Actions](./components/buttons.md) | Standard button variants (`Primary`, `Secondary`, `Success`, `Ghost`) and sizes |
| [Badges & Indicators](./components/badges.md) | Status badges, pills, and indicators |
| [Containers & Cards](./components/containers.md) | Structural components, cards, elevation shadows, and auto-stretching |
| [Forms & Inputs](./components/forms.md) | Form elements, inputs, placeholders, and interactive states |
| [Layout System](./layout.md) | Auto Layout (`flexbox`), explicit geometry, and relational constraints |
| [Renderers](./renderers/index.md) | Target outputs: [HTML](./renderers/html.md), [JSON AST](./renderers/json.md), and [Figma](./renderers/figma.md) |
| [CLI Reference](./cli.md) | Comprehensive manual for all `vireo` commands and flags |
| [Figma Plugin Guide](./figma-plugin.md) | Importing compiled designs directly into Figma canvas |

---

## Why Design as a Code?

Traditional design systems suffer from friction between design canvases and engineering repositories:
1. **Design Drift:** Mockups go stale while code evolves.
2. **AI Agent Inaccessibility:** LLMs and autonomous coding agents cannot reliably read or inspect binary Figma files.
3. **Lack of Version Control:** Binary canvas files do not support standard git branching, reviews, or automated CI testing.

With Vireo, your design system is:
- **100% Machine-Readable:** AI coding agents inspect, generate, and refactor `.dac` files natively.
- **Git-Native:** Designs live alongside your codebase with git diffs, pull requests, and automated validation.
- **Multi-Target:** Compile a single `.dac` specification into live web prototypes, Figma canvas frames, and JSON schemas.