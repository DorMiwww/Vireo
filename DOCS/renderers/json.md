# JSON AST Renderer

The JSON AST Renderer emits a standardized, normalized JSON representation of the parsed and resolved Vireo design tree.

---

## Basic Usage

Render a `.dac` file to JSON using `-o json` or `--json`:

```bash
vireo render button.dac -o json
# Or shorthand:
vireo render button.dac --json
```

---

## Options

### Pretty-Printing & Compact Output

```bash
# Formatted with 2-space indentation (default for inspectability)
vireo render card.dac -o json --pretty

# Compact minified JSON (ideal for piping or automated pipelines)
vireo render card.dac -o json --compact
```

### Direct STDOUT Streaming

Stream JSON directly into other CLI utilities or jq filters:

```bash
vireo render card.dac -o json --stdout | jq '.blocks[0].components[0].properties'
```

---

## AST Schema Overview

The emitted JSON mirrors the core AST structure:

```json
{
  "path": "card.dac",
  "blocks": [
    {
      "name": "Cards",
      "components": [
        {
          "name": "UserProfile",
          "properties": [
            { "key": "width", "value": 320 },
            { "key": "padding", "value": "24" },
            { "key": "color", "value": "#FFFFFF" },
            { "key": "radius", "value": 12 },
            { "key": "shadow", "value": "true" }
          ],
          "constraints": [
            { "type": "AutoLayout", "direction": "VERTICAL", "gap": 16 }
          ],
          "children": [
            {
              "name": "Avatar",
              "properties": [
                { "key": "width", "value": 48 },
                { "key": "height", "value": 48 },
                { "key": "radius", "value": 24 },
                { "key": "color", "value": "#3B82F6" }
              ],
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

---

## Use Cases for JSON AST

1. **AI Agents:** Large Language Models can parse the JSON tree to analyze design system compliance, extract color palettes, or generate frontend component code (React, Vue, SwiftUI, Jetpack Compose).
2. **Design Linters:** Write custom automated rules checking contrast ratios, naming conventions, or spacing consistency.
3. **Custom Export Pipelines:** Ingest Vireo designs into custom company renderers or CMS engines.
