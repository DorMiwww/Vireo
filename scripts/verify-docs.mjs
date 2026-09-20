import fs from "fs";

try {
  const content = fs.readFileSync("DOCS/cli.md", "utf8");

  const requiredTerms = [
    "vireo render",
    "vireo check",
    "vireo init",
    "vireo version",
    "-o, --output <format>",
    "--standard-html",
    "--snippet",
    "--title",
    "--theme",
    "--figma",
    "--pretty",
    "--compact",
    "kubectl"
  ];

  for (const term of requiredTerms) {
    if (!content.includes(term)) {
      throw new Error(`DOCS/cli.md is missing required section or term: "${term}"`);
    }
  }

  console.log("CLI DOCS VERIFIED");
  process.exit(0);
} catch (e) {
  console.error("Documentation verification failed:", e.message);
  process.exit(1);
}
