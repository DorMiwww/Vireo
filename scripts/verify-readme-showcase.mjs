#!/usr/bin/env node
import fs from "fs";
import path from "path";

const readmePath = path.resolve(process.cwd(), "README.md");
const content = fs.readFileSync(readmePath, "utf-8");

const requiredTokens = [
  "showcase/product.dac",
  "showcase/media-stream.dac",
  "showcase/card.dac",
  "DOCS/images/preview-product.png",
  "DOCS/images/preview-media.png",
  "DOCS/images/preview-card.png",
  "showcase/product.html",
  "showcase/media-stream.html"
];

for (const token of requiredTokens) {
  if (!content.includes(token)) {
    console.error(`README.md missing required showcase token: ${token}`);
    process.exit(1);
  }
}

console.log("README SHOWCASE SECTION VERIFIED OK");
process.exit(0);
