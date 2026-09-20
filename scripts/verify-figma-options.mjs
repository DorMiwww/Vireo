import { execSync } from "child_process";

try {
  // 1. Shorthand --figma (pretty)
  const prettyFigma = execSync("./gradlew -q run --args=\"render designs/card.dac --figma\"", { encoding: "utf8" });
  const parsedPretty = JSON.parse(prettyFigma);
  if (!parsedPretty.nodes || parsedPretty.nodes[0]?.type !== "CANVAS") {
    throw new Error("Invalid Figma document structure");
  }
  if (!prettyFigma.includes("{\n  ")) {
    throw new Error("Expected pretty indented JSON");
  }

  // 2. Kubectl style -o figma
  const kubectlFigma = execSync("./gradlew -q run --args=\"render designs/card.dac -o figma\"", { encoding: "utf8" });
  const parsedKubectl = JSON.parse(kubectlFigma);
  if (parsedKubectl.name !== "designs/card.dac") {
    throw new Error("Kubectl -o figma name mismatch");
  }

  // 3. Compact mode
  const compactFigma = execSync("./gradlew -q run --args=\"render designs/card.dac --figma --compact\"", { encoding: "utf8" });
  const parsedCompact = JSON.parse(compactFigma);
  if (parsedCompact.nodes.length === 0) {
    throw new Error("Compact JSON nodes empty");
  }
  if (compactFigma.includes("{\n  ")) {
    throw new Error("Compact JSON should not contain multi-line indentation");
  }

  console.log("FIGMA OPTIONS VERIFIED");
  process.exit(0);
} catch (e) {
  console.error("Figma options verification failed:", e.message);
  process.exit(1);
}
