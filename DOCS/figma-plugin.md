# Vireo Figma Plugin Guide

This document explains how to bridge the Vireo DaaC compiler with Figma using the **Vireo Importer** development plugin.

---

## Background & Architecture

The Figma public REST API does not allow external REST clients to create new frames or shapes on a user's canvas. The standard mechanism to inject design nodes programmatically is via a **Figma Development Plugin**. 

Vireo provides a complete two-step workflow:
1. **Compiler Stage (`vireo-cli` & `vireo-renderer-figma`)**: Compiles `.dac` design specifications into Figma-compatible JSON AST trees (`*.figma.json`).
2. **Canvas Stage (`figma-plugin`)**: A private development plugin running inside Figma Desktop that imports the AST and generates native Figma canvas layers (Frames, Text, Auto Layout, Fills, Strokes).

No Figma marketplace review, hosting, or external services are needed.

---

## Step 1: Generate the Figma AST JSON

From your terminal in the Vireo workspace, compile any `.dac` file using the `render` command with `--to figma`:

```bash
# Example: compile login form design to Figma JSON
./gradlew run --args="render designs/login.dac --to figma -o designs/login.figma.json"
```

You can inspect `designs/login.figma.json` to see the generated node tree with Auto Layout properties, paddings, bounds, and typography settings.

---

## Step 2: Install the Plugin in Figma Desktop

> [!NOTE]
> This step only needs to be performed once in Figma Desktop.

1. Launch **Figma Desktop**.
2. Open any existing design file or create a **New design file**.
3. In the top-left menu bar, click the **Figma logo** (or press `Cmd+/` / `Ctrl+/`).
4. Navigate to:
   **Plugins** → **Development** → **Import plugin from manifest...**
5. In the file picker dialog, navigate to the Vireo project directory and select:
   ```
   figma-plugin/manifest.json
   ```
6. A notification will appear in Figma confirming: *"Plugin imported: Vireo Importer"*.

---

## Step 3: Import and Render on Canvas

1. In your Figma design file, right-click on the canvas and choose:
   **Plugins** → **Development** → **Vireo Importer**
2. In the plugin modal:
   - Choose **File Upload** and pick `designs/login.figma.json` (or paste JSON directly).
   - Select a **Device / Canvas Frame** preset:
     - `None (Component Only)` — imports the component at its native dimensions.
     - `iPhone 16 / 15 Pro (393 × 852)` — wraps the component inside an iPhone device frame with centered auto-layout and background.
     - `Desktop Browser (1440 × 900)` — wraps the component inside a desktop browser canvas.
     - `Google Pixel 7`, `Laptop`, or `iPad Air`.
3. Click **Import to Canvas**.
4. The plugin will instantiate the nodes, load fonts (Inter Regular / Bold), set up Auto Layout containers, and center the viewport on your design.

---

## How to Wrap into Frames Natively in Figma

If you import with `None (Component Only)` and want to place it inside a device frame using Figma's native tools:

1. Press `F` on your keyboard (or click the **Frame** tool in the top toolbar).
2. Look at the **right-hand inspector panel** under **Frame Presets**:
   - **Phone**: Select `iPhone 16 Pro` (or any device).
   - **Desktop**: Select `Desktop (1440 × 1024)`.
3. A blank device frame will appear on the canvas.
4. Drag your imported `LoginForm` inside the device frame.
5. In the right panel, turn on **Auto Layout** (`Shift+A`) on the device frame and set alignment to **Align center** (`Center / Center`).

## Supported Features in Figma

| Feature | Vireo `.dac` Syntax | Figma Representation |
| :--- | :--- | :--- |
| **Auto Layout** | `layout: vertical`, `gap: 16` | Frame Auto Layout `VERTICAL`, `itemSpacing = 16` |
| **Padding** | `padding: 24` or `24 16` | `paddingLeft`, `paddingRight`, `paddingTop`, `paddingBottom` |
| **Sizing** | `width: fill`, `width: 360` | Auto Layout sizing (`FIXED` vs `AUTO`) & explicit dimensions |
| **Typography** | `fontSize: 24`, `fontWeight: bold` | `TextNode` with font family and style weights |
| **Fills & Colors** | `color: #3B82F6` | Solid paint fill with RGBA normalized color |
| **Borders** | `border: 1 #D1D5DB` | Solid stroke with weight and color |
| **Corner Radius** | `radius: 8` | Frame or rectangle `cornerRadius` |
