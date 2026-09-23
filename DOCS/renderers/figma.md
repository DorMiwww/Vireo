# Figma Renderer & Workflow

The Figma Renderer compiles `.dac` design specifications into an intermediate AST payload optimized for ingestion into native Figma canvas frames, Auto Layout containers, and text layers.

---

## The Two-Stage Figma Workflow

Because the Figma public REST API does not allow external scripts to create or inject arbitrary frames into an open user canvas, Vireo uses a secure two-stage model:

1. **Compilation (Vireo CLI):** Compiles `.dac` into `*.figma.json`.
2. **Canvas Ingestion (Figma Desktop Plugin):** A private local development plugin reads the JSON and instantiates native Figma elements.

---

## 1. Generating Figma AST

Compile a `.dac` file with `-o figma` or `--figma`:

```bash
vireo render button.dac -o figma
# Or shorthand:
vireo render button.dac --figma
```

This writes `button.figma.json` containing:
- Exact geometry (x, y, width, height)
- Auto Layout directions, gap, and padding
- Hex color fills and borders
- Typography specifications and font family weights
- Layer hierarchy and component names

---

## 2. Ingesting into Figma

To inject the compiled design onto your Figma canvas:
1. Open Figma Desktop.
2. Load the development plugin from `figma-plugin/manifest.json`.
3. Select your generated `*.figma.json` file.
4. The plugin automatically generates real, editable Figma frames and components.

For step-by-step screenshots and detailed setup instructions, see the [Figma Plugin Guide](../figma-plugin.md).
