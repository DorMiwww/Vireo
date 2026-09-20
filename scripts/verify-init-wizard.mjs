import { execSync } from "child_process";
import fs from "fs";
import path from "path";
import os from "os";

const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "vireo_init_test_"));

try {
  const initOutput = execSync(`./gradlew -q run --args="init TestProject -d ${tempDir} --force"`, { encoding: "utf8" });
  if (!initOutput.includes("Successfully initialized Vireo project 'TestProject'")) {
    throw new Error("Init output missing success message");
  }

  const expectedFiles = [
    "vireo.config.json",
    "designs/tokens.dac",
    "designs/components/button.dac",
    "designs/card.dac",
    "README.md"
  ];

  for (const f of expectedFiles) {
    const fullPath = path.join(tempDir, f);
    if (!fs.existsSync(fullPath)) {
      throw new Error(`Expected initialized file missing: ${f}`);
    }
  }

  // Verify that the generated card.dac passes check
  const cardPath = path.join(tempDir, "designs/card.dac");
  const checkOutput = execSync(`./gradlew -q run --args="check ${cardPath}"`, { encoding: "utf8" });
  if (!checkOutput.includes("passed analysis.")) {
    throw new Error("Generated card.dac failed analysis");
  }

  // Verify that the generated card.dac renders to standard HTML
  const htmlOutput = execSync(`./gradlew -q run --args="render ${cardPath} --html"`, { encoding: "utf8" });
  if (!htmlOutput.startsWith("<!DOCTYPE html>") || !htmlOutput.includes("Welcome to Vireo")) {
    throw new Error("Generated card.dac failed to render valid HTML");
  }

  console.log("INIT WIZARD VERIFIED");
  process.exit(0);
} catch (e) {
  console.error("Init wizard verification failed:", e.message);
  process.exit(1);
} finally {
  try {
    fs.rmSync(tempDir, { recursive: true, force: true });
  } catch (_) {}
}
