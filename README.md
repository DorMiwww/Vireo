# Vireo

> **Design as Code (DaaC).** Write declarative design files (`.dac`) and compile them into HTML, JSON IR, and native Figma Auto Layout components.

[![Build](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)]()
[![Figma Plugin](https://img.shields.io/badge/Figma-Plugin%20Supported-purple)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

Vireo bridges the gap between codebases and design systems. Instead of manually drawing components in Figma and re-implementing them in code, you define your design system in a clean, declarative `.dac` syntax that compiles into:

- **HTML** — Standalone HTML5 previews with native CSS Flexbox styling.
- **JSON IR** — Structured intermediate representation for AST inspection and tooling integrations.
- **Figma** — Real frames, shapes, typography, and Auto Layout structures imported directly into your Figma canvas via the bundled **Vireo Importer** development plugin.

---

## Example Syntax (`.dac`)

```dac
import buttons from "./components/buttons.dac"
import badges from "./components/badges.dac"

block Profile {
    component Card {
        width: 380
        color: #FFFFFF
        radius: 16
        shadow: true
        padding: 24
        layout: vertical
        gap: 16

        component Header {
            layout: horizontal
            gap: 16
            alignItems: center

            component Avatar {
                width: 56
                height: 56
                radius: 99
                color: #2563EB

                component Initials {
                    text: "JD"
                    fontSize: 20
                    fontWeight: bold
                    color: #FFFFFF
                }
            }

            component UserInfo {
                layout: vertical
                gap: 4

                component Name {
                    text: "Jane Doe"
                    fontSize: 18
                    fontWeight: bold
                    color: #111827
                }

                component Handle {
                    text: "@janedoe • Design Systems"
                    fontSize: 13
                    color: #6B7280
                }
            }
        }

        component Bio {
            text: "Design systems architect and UI engineer."
            fontSize: 14
            color: #4B5563
        }

        component ActionBtn {
            ref: buttons.Primary.Default
            label: "Connect"
        }
    }
}
```

---

## Architecture & Modules

Vireo is built as a pure Kotlin (JVM) multi-module project with strict dependency isolation:

```
Vireo
├── vireo-core              # AST model, SourceLocation, Visitor pattern, VireoResult
├── vireo-lexer             # Stateless lexical scanner with token classification
├── vireo-parser            # Recursive descent parser producing unresolved AST
├── vireo-analysis          # Semantic analyzer, symbol table, cross-file imports, expression evaluator
├── vireo-renderer-json     # Structured JSON IR emitter
├── vireo-renderer-html     # Standalone HTML5 + CSS Flexbox generator
├── vireo-renderer-figma    # AST-to-Figma schema transformer
├── vireo-cli               # CLI interface (vireo check, vireo render)
└── figma-plugin            # Native Figma development plugin (JS/HTML) for canvas import
```

Data compilation pipeline:
```
.dac source ──▶ [ Lexer ] ──▶ [ Parser ] ──▶ [ Analyzer ] ──▶ [ Renderer ] ──▶ HTML / JSON / Figma
```

---

## Getting Started

### Prerequisites
- JDK 17+ (JDK 21 recommended)
- Gradle (handled automatically via `./gradlew`)
- Node.js (optional, for the Figma plugin)

### Build & Run Tests
```bash
./gradlew test
```

### CLI Usage

The CLI supports checking syntax and rendering designs into multiple formats:

```bash
# 1. Syntax analysis and import validation
./gradlew :vireo-cli:run --args="check showcase/card.dac"

# 2. Render to standalone HTML
./gradlew :vireo-cli:run --args="render showcase/card.dac --to html -o showcase/card.html"

# 3. Render to Figma JSON
./gradlew :vireo-cli:run --args="render showcase/card.dac --to figma -o showcase/card.figma.json"
```

---

## Figma Integration

Vireo includes a private development plugin located in [`figma-plugin/`](figma-plugin/):

1. Open **Figma Desktop**.
2. Navigate to **Plugins** → **Development** → **Import plugin from manifest...**
3. Select [`figma-plugin/manifest.json`](figma-plugin/manifest.json).
4. Run **Vireo Importer** on your canvas.
5. Choose an optional device canvas preset (*Desktop Browser*, *Laptop*, *iPhone 16*, *iPad*, or *None*).
6. Drag & drop or paste your generated `*.figma.json` file.
7. Click **Import to Canvas** — native Auto Layout frames, text layers, colors, and paddings are rendered directly onto your Figma canvas.

---

## Showcase Designs

A suite of ready-to-test design components is available in the [`showcase/`](showcase/) directory:

- **Buttons & Badges**: [`showcase/components/buttons.dac`](showcase/components/buttons.dac), [`showcase/components/badges.dac`](showcase/components/badges.dac)
- **Profile Card**: [`showcase/card.dac`](showcase/card.dac)
- **SaaS Pricing Table**: [`showcase/pricing.dac`](showcase/pricing.dac)
- **Large SaaS Dashboard**: [`showcase/dashboard.dac`](showcase/dashboard.dac) — Complete responsive analytics dashboard with navigation, hero banner, metrics grid, data tables, and sidebar widgets.

You can preview the pre-rendered HTML files directly:
```bash
open showcase/dashboard.html
```

---

## Roadmap

- [x] **Phase 1**: Core AST, Stateless Lexer & Recursive Descent Parser
- [x] **Phase 2**: Cross-file Imports (`import ... from`), Layout Constraints & HTML5/CSS Flexbox Renderer
- [x] **Phase 3**: Variables, Function Definitions, Arithmetic Expressions, and Conditionals (`if ... then ... else`)
- [x] **Phase 4**: Figma Schema Renderer (`vireo-renderer-figma`)
- [x] **Phase 4.5**: Native Figma Canvas Plugin (`figma-plugin/`) with Auto Layout & Device Canvas Presets
- [ ] **Phase 5**: Interactive Project Scaffolding (`vireo init`)
- [ ] **Backlog**: Figma Design Bundle & Canvas Orchestrator (`vireo bundle`)

---

## Contributing & Agent Rules

Project guidelines, syntax references, architecture decision records, and agent skills are organized under [`.agents/`](.agents/):
- **Rules & Architecture**: [`.agents/rules/AGENTS.md`](.agents/rules/AGENTS.md)
- **Roadmap & Context**: [`.agents/rules/ROADMAP.md`](.agents/rules/ROADMAP.md), [`.agents/rules/CONTEXT.md`](.agents/rules/CONTEXT.md)
- **Agent Skills**: [`.agents/skills/`](.agents/skills/)

---

## License

[MIT License](LICENSE)