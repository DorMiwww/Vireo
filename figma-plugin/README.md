# Vireo Importer — Figma Development Plugin

A private local Figma Development Plugin to render Vireo `*.figma.json` design files directly onto any active Figma canvas.

---

## What It Does

1. **Auto Layout**: Maps Vireo vertical and horizontal stacks to Figma Auto Layout (direction, gaps, paddings, hugging, and fixed sizing).
2. **Components & Frames**: Creates hierarchical frames and groups.
3. **Typography**: Loads font weights (Regular, Medium, Bold), applies font sizes and colors.
4. **Styles**: Translates hex colors, alpha channels, border strokes, and corner radiuses.

---

## How to Install in Figma Desktop

1. Open **Figma Desktop**.
2. Go to: **Menu (Figma logo)** → **Plugins** → **Development** → **Import plugin from manifest...**
3. Select the `manifest.json` file inside this `figma-plugin/` directory.

The plugin **Vireo Importer** will now be available in your development plugins list.

---

## How to Use

1. Generate a Figma JSON file from any `.dac` design using the Vireo CLI:
   ```bash
   ./gradlew run --args="render designs/login.dac --to figma -o designs/login.figma.json"
   ```
2. In Figma Desktop, right-click anywhere on the canvas:
   **Plugins** → **Development** → **Vireo Importer**
3. Drag-and-drop or select `designs/login.figma.json` (or paste the JSON text directly).
4. Click **Import to Canvas**.
5. The design appears on your canvas with all Auto Layout containers and styles configured!
