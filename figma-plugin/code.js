// Vireo Importer - Figma Development Plugin
// Converts Vireo *.figma.json AST into native Figma canvas layers.

figma.showUI(__html__, { width: 380, height: 440, title: "Vireo Importer" });

// Highest *.figma.json schemaVersion this plugin knows how to read.
// Bump only when this file is updated to handle a newer node shape from the CLI
// (see FigmaDocument.SCHEMA_VERSION in vireo-renderer-figma).
const SUPPORTED_SCHEMA_VERSION = 2;

const loadedFonts = new Set();

async function ensureFont(family, style) {
  const fontKey = `${family}::${style}`;
  if (loadedFonts.has(fontKey)) return true;

  try {
    await figma.loadFontAsync({ family, style });
    loadedFonts.add(fontKey);
    return true;
  } catch (err) {
    // Try fallback to Inter with same style
    try {
      await figma.loadFontAsync({ family: "Inter", style });
      loadedFonts.add(`Inter::${style}`);
      return false;
    } catch (err2) {
      // Fallback to Inter Regular
      try {
        await figma.loadFontAsync({ family: "Inter", style: "Regular" });
        loadedFonts.add("Inter::Regular");
        return false;
      } catch (err3) {
        return false;
      }
    }
  }
}

function base64ToBytes(base64Str) {
  try {
    const raw = base64Str.replace(/^data:[^;]+;base64,/, '').trim();
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
    const len = raw.length;
    let placeHolders = 0;
    if (raw.endsWith('==')) placeHolders = 2;
    else if (raw.endsWith('=')) placeHolders = 1;
    const bytes = new Uint8Array(Math.max(0, Math.floor((len * 3) / 4) - placeHolders));
    let p = 0;
    for (let i = 0; i < len; i += 4) {
      const a = chars.indexOf(raw[i]);
      const b = chars.indexOf(raw[i + 1]);
      const c = chars.indexOf(raw[i + 2]);
      const d = chars.indexOf(raw[i + 3]);
      if (a === -1 || b === -1) break;
      bytes[p++] = (a << 2) | (b >> 4);
      if (c !== -1 && c !== 64) {
        bytes[p++] = ((b & 15) << 4) | (c >> 2);
      }
      if (d !== -1 && d !== 64) {
        bytes[p++] = ((c & 3) << 6) | d;
      }
    }
    return bytes;
  } catch (e) {
    return null;
  }
}

function mapPaints(paintList, node) {
  if (!paintList || paintList.length === 0) return [];
  const result = [];
  for (const p of paintList) {
    if (p.type === 'IMAGE') {
      let hash = null;
      const b64 = (node && node.imageBase64) || (p.imageRef && p.imageRef.startsWith('data:') ? p.imageRef : null);
      if (b64) {
        const bytes = base64ToBytes(b64);
        if (bytes && bytes.length > 0 && typeof figma.createImage === 'function') {
          try {
            const img = figma.createImage(bytes);
            hash = img.hash;
          } catch (e) {}
        }
      }
      if (hash) {
        result.push({
          type: 'IMAGE',
          scaleMode: p.scaleMode || 'FILL',
          imageHash: hash,
          opacity: (p.opacity !== undefined && p.opacity !== null) ? p.opacity : 1.0
        });
      } else {
        result.push({
          type: 'SOLID',
          color: { r: 0.9, g: 0.92, b: 0.95 },
          opacity: (p.opacity !== undefined && p.opacity !== null) ? p.opacity : 1.0
        });
      }
      continue;
    }
    const col = p.color || { r: 0, g: 0, b: 0 };
    const opacity = (col.a !== undefined && col.a !== null)
      ? col.a
      : ((p.opacity !== undefined && p.opacity !== null) ? p.opacity : 1.0);

    result.push({
      type: 'SOLID',
      color: {
        r: Math.max(0, Math.min(1, col.r)),
        g: Math.max(0, Math.min(1, col.g)),
        b: Math.max(0, Math.min(1, col.b))
      },
      opacity: Math.max(0, Math.min(1, opacity))
    });
  }
  return result;
}

function applyChildLayoutSizing(figmaElement, node, parent) {
  if (node.layoutAlign) {
    figmaElement.layoutAlign = node.layoutAlign;
  }
  if (node.layoutGrow !== undefined && node.layoutGrow !== null) {
    figmaElement.layoutGrow = node.layoutGrow;
  }

  // Modern Figma API layout sizing
  try {
    if (node.layoutAlign === 'STRETCH') {
      if (parent && parent.layoutMode === 'VERTICAL') {
        if (figmaElement.type === 'TEXT') {
          figmaElement.textAutoResize = 'HEIGHT';
        }
        figmaElement.layoutSizingHorizontal = 'FILL';
      } else if (parent && parent.layoutMode === 'HORIZONTAL') {
        figmaElement.layoutSizingVertical = 'FILL';
      }
    }
    if (node.layoutGrow === 1) {
      if (parent && parent.layoutMode === 'HORIZONTAL') {
        if (figmaElement.type === 'TEXT') {
          figmaElement.textAutoResize = 'HEIGHT';
        }
        figmaElement.layoutSizingHorizontal = 'FILL';
      } else if (parent && parent.layoutMode === 'VERTICAL') {
        figmaElement.layoutSizingVertical = 'FILL';
      }
    }
  } catch (e) {}
}

async function renderNode(node, parent) {
  if (!node) return null;

  if (node.type === "CANVAS") {
    const created = [];
    let currentX = 0;
    if (node.children) {
      for (const child of node.children) {
        const childNode = await renderNode(child, parent);
        if (childNode) {
          if ('x' in childNode && 'width' in childNode) {
            childNode.x = currentX;
            currentX += childNode.width + 40;
          }
          created.push(childNode);
        }
      }
    }
    return created;
  }

  if (node.mediaType === "SVG" && node.svgContent) {
    try {
      const svgNode = figma.createNodeFromSvg(node.svgContent);
      svgNode.name = node.name || "SVG";
      const bbox = node.absoluteBoundingBox;
      if (bbox && bbox.width > 0 && bbox.height > 0) {
        svgNode.resize(bbox.width, bbox.height);
      }
      if (bbox && parent && (!parent.layoutMode || parent.layoutMode === "NONE")) {
        if (bbox.x !== undefined && bbox.x !== null) svgNode.x = bbox.x;
        if (bbox.y !== undefined && bbox.y !== null) svgNode.y = bbox.y;
      }
      if (parent && parent.appendChild) {
        parent.appendChild(svgNode);
      }
      applyChildLayoutSizing(svgNode, node, parent);
      return svgNode;
    } catch (e) {}
  }

  if (node.type === "FRAME") {
    const frame = figma.createFrame();
    frame.name = node.name || "Frame";

    // Absolute dimensions / bounding box
    const bbox = node.absoluteBoundingBox;
    if (bbox && bbox.width > 0 && bbox.height > 0) {
      frame.resize(bbox.width, bbox.height);
    } else if (bbox && bbox.width > 0) {
      frame.resize(bbox.width, Math.max(frame.height, 44));
    } else if (bbox && bbox.height > 0) {
      frame.resize(Math.max(frame.width, 100), bbox.height);
    }

    if (bbox && parent && (!parent.layoutMode || parent.layoutMode === "NONE")) {
      if (bbox.x !== undefined && bbox.x !== null) frame.x = bbox.x;
      if (bbox.y !== undefined && bbox.y !== null) frame.y = bbox.y;
    }

    // Auto Layout configuration
    if (node.layoutMode === "VERTICAL" || node.layoutMode === "HORIZONTAL") {
      frame.layoutMode = node.layoutMode;
      if (node.itemSpacing !== undefined && node.itemSpacing !== null) {
        frame.itemSpacing = node.itemSpacing;
      }
      if (node.paddingLeft !== undefined && node.paddingLeft !== null) {
        frame.paddingLeft = node.paddingLeft;
      }
      if (node.paddingRight !== undefined && node.paddingRight !== null) {
        frame.paddingRight = node.paddingRight;
      }
      if (node.paddingTop !== undefined && node.paddingTop !== null) {
        frame.paddingTop = node.paddingTop;
      }
      if (node.paddingBottom !== undefined && node.paddingBottom !== null) {
        frame.paddingBottom = node.paddingBottom;
      }

      if (node.primaryAxisSizingMode) {
        frame.primaryAxisSizingMode = node.primaryAxisSizingMode;
      }
      if (node.counterAxisSizingMode) {
        frame.counterAxisSizingMode = node.counterAxisSizingMode;
      }
      if (node.primaryAxisAlignItems) {
        frame.primaryAxisAlignItems = node.primaryAxisAlignItems;
      }
      if (node.counterAxisAlignItems) {
        frame.counterAxisAlignItems = node.counterAxisAlignItems;
      }
    }

    // Corner Radius
    if (node.cornerRadius !== undefined && node.cornerRadius !== null) {
      frame.cornerRadius = node.cornerRadius;
    }

    // Fills
    if (node.fills && node.fills.length > 0) {
      frame.fills = mapPaints(node.fills, node);
    } else {
      frame.fills = [];
    }

    // Strokes
    if (node.strokes && node.strokes.length > 0) {
      frame.strokes = mapPaints(node.strokes, node);
      if (node.strokeWeight) frame.strokeWeight = node.strokeWeight;
    }

    if (parent && parent.appendChild) {
      parent.appendChild(frame);
    }

    applyChildLayoutSizing(frame, node, parent);

    // Recursively render children
    if (node.children) {
      for (const child of node.children) {
        await renderNode(child, frame);
      }
    }

    return frame;
  }

  if (node.type === "TEXT") {
    const family = (node.style && node.style.fontFamily) || "Inter";
    const weight = (node.style && node.style.fontWeight) || 400;
    let style = "Regular";
    if (weight >= 700) style = "Bold";
    else if (weight >= 500) style = "Medium";
    else if (weight <= 300) style = "Light";

    const fontFound = await ensureFont(family, style);
    const text = figma.createText();
    text.name = node.name || "Text";

    if (fontFound) {
      text.fontName = { family, style };
    } else {
      text.fontName = { family: "Inter", style: style === "Bold" ? "Bold" : "Regular" };
    }

    if (node.style && node.style.fontSize) {
      text.fontSize = node.style.fontSize;
    }

    text.characters = node.characters || "";

    const bbox = node.absoluteBoundingBox;
    if (bbox && bbox.width > 0) {
      text.resize(bbox.width, bbox.height > 0 ? bbox.height : text.height);
      text.textAutoResize = "HEIGHT";
    }

    if (bbox && parent && (!parent.layoutMode || parent.layoutMode === "NONE")) {
      if (bbox.x !== undefined && bbox.x !== null) text.x = bbox.x;
      if (bbox.y !== undefined && bbox.y !== null) text.y = bbox.y;
    }

    if (node.fills && node.fills.length > 0) {
      text.fills = mapPaints(node.fills, node);
    }

    if (node.strokes && node.strokes.length > 0) {
      text.strokes = mapPaints(node.strokes, node);
      if (node.strokeWeight) text.strokeWeight = node.strokeWeight;
    }

    if (node.cornerRadius !== undefined && node.cornerRadius !== null && 'cornerRadius' in text) {
      text.cornerRadius = node.cornerRadius;
    }

    if (parent && parent.appendChild) {
      parent.appendChild(text);
    }

    applyChildLayoutSizing(text, node, parent);

    return text;
  }

  if (node.type === "RECTANGLE") {
    const rect = figma.createRectangle();
    rect.name = node.name || "Rectangle";

    const bbox = node.absoluteBoundingBox;
    const w = bbox && bbox.width > 0 ? bbox.width : 100;
    const h = bbox && bbox.height > 0 ? bbox.height : 44;
    rect.resize(w, h);

    if (bbox && parent && (!parent.layoutMode || parent.layoutMode === "NONE")) {
      if (bbox.x !== undefined && bbox.x !== null) rect.x = bbox.x;
      if (bbox.y !== undefined && bbox.y !== null) rect.y = bbox.y;
    }

    if (node.cornerRadius !== undefined && node.cornerRadius !== null) {
      rect.cornerRadius = node.cornerRadius;
    }

    if (node.fills && node.fills.length > 0) {
      rect.fills = mapPaints(node.fills, node);
    } else {
      rect.fills = [];
    }

    if (node.strokes && node.strokes.length > 0) {
      rect.strokes = mapPaints(node.strokes, node);
      if (node.strokeWeight) rect.strokeWeight = node.strokeWeight;
    }

    if (parent && parent.appendChild) {
      parent.appendChild(rect);
    }

    applyChildLayoutSizing(rect, node, parent);

    return rect;
  }

  return null;
}

const PRESETS = {
  iphone16: { name: "iPhone 16 / 15 Pro", width: 393, height: 852, radius: 50, bg: { r: 0.95, g: 0.96, b: 0.98 } },
  browser: { name: "Desktop Browser", width: 1440, height: 900, radius: 0, bg: { r: 0.94, g: 0.95, b: 0.97 } },
  laptop: { name: "Laptop (1280 × 800)", width: 1280, height: 800, radius: 0, bg: { r: 0.94, g: 0.95, b: 0.97 } },
  android: { name: "Google Pixel 7", width: 412, height: 915, radius: 36, bg: { r: 0.95, g: 0.96, b: 0.98 } },
  ipad: { name: "iPad Air", width: 820, height: 1180, radius: 24, bg: { r: 0.95, g: 0.96, b: 0.98 } }
};

figma.ui.onmessage = async (msg) => {
  if (msg.type === 'IMPORT_JSON') {
    try {
      const payload = msg.payload;
      const preset = msg.preset || 'none';

      if (!payload) {
        figma.ui.postMessage({ type: 'ERROR', error: 'Empty JSON payload' });
        return;
      }

      // schemaVersion is absent on files generated before this check existed —
      // those still match today's node shape, so treat missing as compatible.
      if (typeof payload.schemaVersion === 'number' && payload.schemaVersion > SUPPORTED_SCHEMA_VERSION) {
        const msg = `This design file needs a newer version of the Vireo plugin ` +
          `(file schemaVersion ${payload.schemaVersion}, plugin supports up to ${SUPPORTED_SCHEMA_VERSION}). ` +
          `Re-import the plugin from the latest vireo repo (Plugins → Development → Import from manifest).`;
        figma.notify('Vireo: plugin is out of date for this file', { error: true });
        figma.ui.postMessage({ type: 'ERROR', error: msg });
        return;
      }

      let rootNodes = [];
      if (payload.nodes && Array.isArray(payload.nodes)) {
        rootNodes = payload.nodes;
      } else if (Array.isArray(payload)) {
        rootNodes = payload;
      } else if (payload.type) {
        rootNodes = [payload];
      } else {
        figma.ui.postMessage({ type: 'ERROR', error: 'Unrecognized Vireo document schema' });
        return;
      }

      const createdItems = [];
      for (const rootNode of rootNodes) {
        const res = await renderNode(rootNode, figma.currentPage);
        if (Array.isArray(res)) {
          createdItems.push(...res);
        } else if (res) {
          createdItems.push(res);
        }
      }

      if (createdItems.length > 0) {
        let finalSelection = createdItems;

        // Wrap in Canvas/Device Preset if chosen
        if (preset && preset !== 'none' && PRESETS[preset]) {
          const p = PRESETS[preset];
          const deviceFrame = figma.createFrame();
          deviceFrame.name = p.name;
          deviceFrame.resize(p.width, p.height);
          if (p.radius) {
            deviceFrame.cornerRadius = p.radius;
          }
          deviceFrame.fills = [{ type: 'SOLID', color: p.bg }];

          for (const item of createdItems) {
            deviceFrame.appendChild(item);
          }

          const totalContentHeight = createdItems.reduce((acc, item) => acc + (item.height || 0), 0);
          const isPage = createdItems.some(item => (item.name || "").toLowerCase().includes("page") || (item.name || "").toLowerCase().includes("screen"));
          const isTall = totalContentHeight > (p.height - 80);

          deviceFrame.layoutMode = "VERTICAL";
          deviceFrame.counterAxisAlignItems = "CENTER";
          deviceFrame.counterAxisSizingMode = "FIXED";

          try {
            deviceFrame.minWidth = p.width;
            deviceFrame.minHeight = p.height;
          } catch (e) {}

          if (isTall) {
            // Tall/multi-element layout: top-align and stretch canvas vertically
            deviceFrame.primaryAxisAlignItems = "MIN";
            deviceFrame.primaryAxisSizingMode = "AUTO";
            deviceFrame.paddingTop = 40;
            deviceFrame.paddingBottom = 60;
            deviceFrame.clipsContent = p.radius ? true : false;
          } else if (isPage) {
            // Page layout that fits within preset viewport: top-align with standard padding and full preset height
            deviceFrame.primaryAxisAlignItems = "MIN";
            deviceFrame.primaryAxisSizingMode = "FIXED";
            deviceFrame.resize(p.width, p.height);
            deviceFrame.paddingTop = 40;
            deviceFrame.paddingBottom = 40;
            deviceFrame.clipsContent = p.radius ? true : false;
          } else {
            // Standalone component/widget: centered in full preset device canvas
            deviceFrame.primaryAxisAlignItems = "CENTER";
            deviceFrame.primaryAxisSizingMode = "FIXED";
            deviceFrame.resize(p.width, p.height);
            deviceFrame.clipsContent = p.radius ? true : false;
          }

          figma.currentPage.appendChild(deviceFrame);
          finalSelection = [deviceFrame];
        }

        figma.currentPage.selection = finalSelection;
        figma.viewport.scrollAndZoomIntoView(finalSelection);
        figma.notify(`Vireo: Successfully imported ${finalSelection.length} design(s)!`);
        figma.ui.postMessage({
          type: 'SUCCESS',
          message: `Successfully imported to canvas!`
        });
      } else {
        figma.ui.postMessage({ type: 'ERROR', error: 'No renderable canvas nodes found in JSON.' });
      }
    } catch (err) {
      figma.ui.postMessage({ type: 'ERROR', error: 'Import failed: ' + (err.message || err) });
    }
  }
};
