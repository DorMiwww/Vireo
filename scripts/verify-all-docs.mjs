import fs from "fs";
import path from "path";

const docsRoot = path.resolve("DOCS");

function checkSummary() {
  const summaryPath = path.join(docsRoot, "SUMMARY.md");
  if (!fs.existsSync(summaryPath)) {
    throw new Error("DOCS/SUMMARY.md does not exist");
  }
  const content = fs.readFileSync(summaryPath, "utf8");
  const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;
  let match;
  const linkedFiles = [];

  while ((match = linkRegex.exec(content)) !== null) {
    const rawTarget = match[2].trim();
    if (rawTarget.startsWith("http://") || rawTarget.startsWith("https://")) {
      continue;
    }
    const cleanTarget = rawTarget.split("#")[0].split("?")[0];
    if (!cleanTarget) continue;

    const resolved = path.resolve(docsRoot, cleanTarget);
    if (!fs.existsSync(resolved)) {
      throw new Error(`SUMMARY.md links to non-existent file: "${rawTarget}" (resolved: ${resolved})`);
    }
    const stat = fs.statSync(resolved);
    if (stat.size < 50) {
      throw new Error(`Referenced file is too small or placeholder: "${resolved}" (${stat.size} bytes)`);
    }
    linkedFiles.push(cleanTarget);
  }

  if (linkedFiles.length < 8) {
    throw new Error(`Expected at least 8 structured doc pages in SUMMARY.md, found ${linkedFiles.length}`);
  }
}

function checkAllMarkdownLinks(dir = docsRoot) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      checkAllMarkdownLinks(fullPath);
    } else if (entry.isFile() && entry.name.endsWith(".md")) {
      const content = fs.readFileSync(fullPath, "utf8");
      const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;
      let match;

      while ((match = linkRegex.exec(content)) !== null) {
        const rawTarget = match[2].trim();
        if (rawTarget.startsWith("http://") || rawTarget.startsWith("https://") || rawTarget.startsWith("mailto:")) {
          continue;
        }
        const cleanTarget = rawTarget.split("#")[0].split("?")[0];
        if (!cleanTarget) continue;

        const resolved = path.resolve(dir, cleanTarget);
        if (!fs.existsSync(resolved)) {
          throw new Error(`Broken link in "${path.relative(docsRoot, fullPath)}": "${rawTarget}" -> file not found at "${resolved}"`);
        }
      }
    }
  }
}

function checkGitBookConfigs() {
  const gitbookDocs = path.join(docsRoot, "gitbook-docs.yaml");
  if (!fs.existsSync(gitbookDocs)) {
    throw new Error("DOCS/gitbook-docs.yaml missing");
  }
  const gbdContent = fs.readFileSync(gitbookDocs, "utf8");
  if (!gbdContent.includes("$schema: https://api.gitbook.com/gitbook-docs.yaml")) {
    throw new Error("DOCS/gitbook-docs.yaml missing required schema header");
  }

  const gitbookSpace = path.join(docsRoot, ".gitbook.yaml");
  if (!fs.existsSync(gitbookSpace)) {
    throw new Error("DOCS/.gitbook.yaml missing");
  }
  const gbsContent = fs.readFileSync(gitbookSpace, "utf8");
  if (!gbsContent.includes("readme: index.md")) {
    throw new Error("DOCS/.gitbook.yaml must set readme: index.md");
  }
}

try {
  checkSummary();
  checkAllMarkdownLinks();
  checkGitBookConfigs();
  console.log("ALL DOCS AND LINKS VERIFIED OK");
  process.exit(0);
} catch (err) {
  console.error("Documentation verification failed:", err.message);
  process.exit(1);
}
