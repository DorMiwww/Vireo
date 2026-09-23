---
name: vireo
description: |
  Specialized skill for Graphic & Visual Design as Code (DaaC) using Vireo (.dac files).
  Activate whenever working with .dac files, Vireo compiler, visual UI design, screen mockups, component design systems, tokens, Auto Layout, canvas styling, or code-driven Figma/HTML visual assets.
  Strictly for GRAPHIC and VISUAL design as code (UI layouts, cards, buttons, forms, typography, colors, responsive containers) — NOT for software backend system architecture or database design.
  Dynamically fetches current syntax, component models, and CLI properties from https://dormiwww.github.io/Vireo/ with zero hardcoding.
metadata:
  author: Vireo
  version: '2.1.0'
  docs_url: 'https://dormiwww.github.io/Vireo/'
  keywords:
    - dac
    - vireo
    - design-as-code
    - daac
    - visual-design
    - graphic-design
    - ui-design
    - design-system
    - figma-code
---

# Vireo Design as a Code (`.dac`) Skill

This skill equips AI agents (Claude, Antigravity, Gemini) to author, refactor, and architect professional `.dac` (Design as a Code) files using **live, dynamically fetched documentation**.

---

## 🎯 When to Activate This Skill

Activate this skill when:
- Working with **`.dac` files** or the **Vireo** toolchain.
- The user requests **"Design as a Code" / "дизайн як код"** for graphic, visual, or UI layouts.
- Creating or editing visual UI components (buttons, input fields, badges, cards, navigation, headers, footers).
- Building screen mockups, dashboard layouts, mobile/web interfaces in code.
- Managing design tokens (color palettes, typography scales, spacing, border radii).
- Exporting or rendering design code to HTML previews (`vireo render -o html`) or Figma canvases (`vireo render -o figma`).

> [!NOTE]
> **Domain boundary:** This skill is strictly for **visual and graphic design as code**. Do NOT use this skill for backend software system architecture, database design, or distributed systems.

---

## ⚠️ ZERO-HARDCODING DIRECTIVE

> [!IMPORTANT]
> **DO NOT USE HARDCODED SYNTAX OR IMAGINED PROPERTY NAMES.**
> The `.dac` language, supported component properties, layout rules, and CLI flags are actively developed and updated. Never rely on training memory or static assumptions.
> 
> **You MUST dynamically fetch the relevant documentation from the official site before authoring or modifying `.dac` files:**
> 👉 **`https://dormiwww.github.io/Vireo/`**
>
> *(If working in a repository that contains a local `DOCS/` folder, read the local markdown files directly from `DOCS/`; otherwise, fetch the live URLs below using your URL/HTTP tool).*

---

## 1. Documentation Sitemap (Fetch as Needed)

Use your URL reading tool (`read_url_content`) or file viewer to retrieve the exact section required for the user's task:

| Domain | Live Documentation URL | Local Path (if in Vireo repo) | When to Fetch |
| :--- | :--- | :--- | :--- |
| **Overview & Index** | `https://dormiwww.github.io/Vireo/` | `DOCS/index.md` | Understanding high-level platform status and table of contents |
| **Getting Started** | `https://dormiwww.github.io/Vireo/getting-started` | `DOCS/getting-started.md` | Onboarding, scaffolding with `vireo init`, first `.dac` file |
| **Core Concepts** | `https://dormiwww.github.io/Vireo/concepts/` | `DOCS/concepts/index.md` | DaaC philosophy, compiler pipeline, file/block structure |
| **Addressing & `ref:`** | `https://dormiwww.github.io/Vireo/concepts/addressing` | `DOCS/concepts/addressing.md` | Cross-file imports, dot-notation addressing, `ref:` inheritance |
| **Variables & Expressions**| `https://dormiwww.github.io/Vireo/concepts/expressions`| `DOCS/concepts/expressions.md` | Tokens via `var`, `$variable`, functions, conditionals |
| **Component Model** | `https://dormiwww.github.io/Vireo/components/` | `DOCS/components/index.md` | Component hierarchy, nesting, DOM/Figma mapping |
| **Properties Catalogue** | `https://dormiwww.github.io/Vireo/components/properties` | `DOCS/components/properties.md` | Exhaustive catalogue of all valid property keys, types, and values |
| **Buttons & Actions** | `https://dormiwww.github.io/Vireo/components/buttons` | `DOCS/components/buttons.md` | Button patterns, variants (Primary, Ghost, etc.), sizing |
| **Badges & Indicators** | `https://dormiwww.github.io/Vireo/components/badges` | `DOCS/components/badges.md` | Status pills, indicator dots, tags |
| **Containers & Cards** | `https://dormiwww.github.io/Vireo/components/containers` | `DOCS/components/containers.md` | Card surfaces, elevation shadows, borders, full sections |
| **Forms & Inputs** | `https://dormiwww.github.io/Vireo/components/forms` | `DOCS/components/forms.md` | Input fields, placeholders, labels, auth forms |
| **Layout & Constraints** | `https://dormiwww.github.io/Vireo/layout` | `DOCS/layout.md` | Auto Layout, flexbox direction, gap, fill/hug, positioning |
| **Renderers Overview** | `https://dormiwww.github.io/Vireo/renderers/` | `DOCS/renderers/index.md` | Compilation pipeline, target formats |
| **HTML Renderer** | `https://dormiwww.github.io/Vireo/renderers/html` | `DOCS/renderers/html.md` | HTML options (`--standard-html`, `--snippet`, `--theme`) |
| **JSON AST** | `https://dormiwww.github.io/Vireo/renderers/json` | `DOCS/renderers/json.md` | Machine-readable AST for tooling and inspection |
| **Figma Integration** | `https://dormiwww.github.io/Vireo/renderers/figma` | `DOCS/renderers/figma.md` | Figma AST generation (`--figma`) |
| **Figma Plugin Guide** | `https://dormiwww.github.io/Vireo/figma-plugin` | `DOCS/figma-plugin.md` | Canvas ingestion via Figma Desktop plugin |
| **CLI Reference** | `https://dormiwww.github.io/Vireo/cli` | `DOCS/cli.md` | All `vireo` commands, options, and flags |

---

## 2. Professional Agent Execution Workflow

Whenever the user asks you to create, edit, or refactor a `.dac` file or design architecture, follow this strict protocol:

```
1. Identify Requirements
        │
        ▼
2. Fetch Latest Docs  ────────► Query https://dormiwww.github.io/Vireo/<section>
        │
        ▼
3. Architect System   ────────► Apply Atomic Design & modular separation
        │
        ▼
4. Author .dac Code   ────────► Use exact confirmed syntax and properties
        │
        ▼
5. Validate & Render  ────────► Run `vireo check` & `vireo render`
```

### Step 1: Query the Live Documentation
- Determine which UI elements are needed (e.g., if creating a pricing card with buttons, fetch `components/properties`, `components/containers`, and `components/buttons`).
- Read the live documentation to confirm the exact property names, syntax constructs, and constraints.

### Step 2: Structure Design Based on Best Practices
Structure project files according to atomic, scalable design system patterns:
- **`tokens.dac`:** Centralize design tokens (`var` for colors, spacing scales, border radii, font sizes).
- **`components/*.dac`:** Atomic reusable components (buttons, badges, inputs, cards) grouped into logical `block` scopes.
- **`screens/*.dac` / `layouts/*.dac`:** High-level screens and layouts that import components and compose them using `ref:` and Auto Layout.

### Step 3: Author Clean `.dac` Code
- **Composition over duplication:** Use `ref:` to inherit from base components and override only specific attributes or nested labels.
- **Auto Layout discipline:** Rely on `layout: horizontal` or `layout: vertical` with explicit `gap:` for all content flow. Avoid hardcoded coordinate positioning (`x`, `y`) unless designing floating badges or overlays.
- **Responsive sizing:** Use `fill` for flexible containers/inputs and `hug` for content-driven buttons, tags, and labels.

### Step 4: Validate and Verify with the Compiler
Always verify newly authored code using the Vireo CLI:

```bash
# 1. Statically validate syntax, imports, and references
vireo check <file.dac>

# 2. Render to HTML to inspect preview
vireo render <file.dac> -o html --theme light

# 3. Render to JSON or Figma if requested
vireo render <file.dac> -o figma
vireo render <file.dac> -o json --pretty
```
