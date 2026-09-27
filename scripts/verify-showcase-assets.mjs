#!/usr/bin/env node
import fs from "fs";
import path from "path";

const requiredFiles = [
  "DOCS/images/preview-product.png",
  "DOCS/images/preview-media.png",
  "DOCS/images/preview-card.png",
  "DOCS/images/preview-pricing.png",
  "DOCS/images/preview-dashboard.png",
  "showcase/product.dac",
  "showcase/product.html",
  "showcase/product.figma.json",
  "showcase/media-stream.dac",
  "showcase/media-stream.html",
  "showcase/media-stream.figma.json",
  "showcase/card.dac",
  "showcase/card.html",
  "showcase/card.figma.json"
];

for (const relPath of requiredFiles) {
  const fullPath = path.resolve(process.cwd(), relPath);
  if (!fs.existsSync(fullPath)) {
    console.error(`Missing required showcase file: ${relPath}`);
    process.exit(1);
  }
  const stat = fs.statSync(fullPath);
  if (stat.size < 100) {
    console.error(`File too small or empty: ${relPath} (${stat.size} bytes)`);
    process.exit(1);
  }
}

console.log("ALL SHOWCASE ASSETS VERIFIED OK");
process.exit(0);
