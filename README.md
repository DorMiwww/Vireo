# Vireo

> Design as code. `.dac` → HTML · JSON · Figma.

🚧 **Status: early stage / proof of concept.** The syntax, architecture, and API described below are a working plan, not a finalized standard.

---

## The Idea

Vireo is a custom declarative syntax (`.dac`) for describing UI components as code — where elements live, what layout they use, what variants and properties they expose. One description, three outputs:

- **HTML** — static render for preview and documentation
- **JSON** — machine-readable intermediate representation (IR) for integrations
- **Figma** — real nodes, components, and variants pushed into a design file

The goal: eliminate the manual duplication of work between code and design files, and make the design system a versioned artifact just like the code.

---

## Syntax (draft)

```
component Button {
  variant size: sm, md, lg
  variant style: primary, secondary

  layout: horizontal
  padding: 12 16
  align: center

  prop showIcon: boolean = false
  prop label: text = "Button"
}
```

The syntax is not finalized — it evolves alongside renderer capabilities.

---

## Architecture

Kotlin Multiplatform, split into a compiler and renderers:

```
:core          (commonMain) — lexer / parser .dac → AST → IR
:render-json   (commonMain) — IR → JSON       via kotlinx.serialization
:render-html   (commonMain) — IR → HTML       via kotlinx.html
:render-figma  (jsMain)     — IR → Figma nodes via Kotlin/JS + Figma Plugin API
```

`:core` has zero platform dependencies — tested with plain `kotlin.test`, no Figma or browser required.

Data flow:

```
.dac file → [Compiler: parser + validator] → IR (JSON) → [Renderer] → HTML / JSON / Figma
```

---

## Stack

| Layer             | Technology                                                                                                    |
|-------------------|---------------------------------------------------------------------------------------------------------------|
| Shared core       | Kotlin Multiplatform                                                                                          |
| Serialization     | kotlinx.serialization                                                                                         |
| HTML rendering    | kotlinx.html                                                                                                  |
| Figma integration | Kotlin/JS + Figma Plugin API (`external` declarations written by hand — no ready-made Kotlin bindings exist)  |

---

## Known Technical Risk

Figma plugins run not in a standard browser JS engine but in **QuickJS compiled to WebAssembly** — and by developer reports this version lags behind the current QuickJS release and doesn't support some of its features. Whether a compiled Kotlin/JS runtime will load and run in this sandbox without issues has no confirmed precedent.

**Mitigation:** before building the full `:render-figma`, run a one-day spike — a minimal Kotlin/JS plugin with a single `external` declaration (`figma.createRectangle()`), and verify it loads and executes inside Figma without QuickJS errors.

**Plan B**, if the spike fails: `:core`, `:render-json`, and `:render-html` stay in Kotlin unchanged; `:render-figma` is rewritten as a thin TypeScript layer that only interprets the JSON IR and calls `figma.*` — no business logic.

---

## Figma Renderer Notes

- `figma.createComponentSet()` does not exist — Figma does not support empty component sets
- Variant components are created via `figma.combineAsVariants(components, parent)` on an array of already-created `ComponentNode`s
- Variant axes are encoded in each component's `.name` string (`"size=md, style=primary"`) before combining
- Component properties (boolean / text / instance-swap) are added to each variant **before** `combineAsVariants`, then linked to child layers via `componentPropertyReferences`

---

## MVP Roadmap

- [ ] Spike: Kotlin/JS plugin inside the Figma sandbox (validates the risk above)
- [ ] IR domain model: `ComponentDef`, `VariantAxis`, `PropertyDef`, `LayoutProps`
- [ ] `.dac` parser → AST → IR (`:core`), unit tests without Figma
- [ ] `:render-json` — trivial, kotlinx.serialization
- [ ] `:render-html` — basic renderer via kotlinx.html
- [ ] `:render-figma` — IR → node interpreter, component and variant support
- [ ] End-to-end test: one component (Button, variants `size` × `style`) through the full pipeline

---

## Project Goals

A pet project and open-source tool simultaneously, with a possible shift to a commercial product later. Priority right now: MVP.

---

## License

Not yet decided.

## Contributing

The project is at an early stage — issues and architecture discussions are welcome once a public repository is available.