import { execSync } from "child_process";

try {
  // 1. Shorthand --html (standard HTML document)
  const stdHtml = execSync("./gradlew -q run --args=\"render designs/card.dac --html\"", { encoding: "utf8" });
  if (!stdHtml.startsWith("<!DOCTYPE html>")) {
    throw new Error("--html did not start with <!DOCTYPE html>");
  }
  if (!stdHtml.includes("<title>designs/card.dac</title>")) {
    throw new Error("--html missing expected default title");
  }
  if (!stdHtml.includes("class=\"vireo-file\"")) {
    throw new Error("--html missing vireo-file container");
  }

  // 2. Kubectl style -o html
  const kubectlHtml = execSync("./gradlew -q run --args=\"render designs/card.dac -o html\"", { encoding: "utf8" });
  if (!kubectlHtml.startsWith("<!DOCTYPE html>")) {
    throw new Error("-o html did not start with <!DOCTYPE html>");
  }

  // 3. Snippet mode
  const snippetHtml = execSync("./gradlew -q run --args=\"render designs/card.dac --html --snippet\"", { encoding: "utf8" });
  if (snippetHtml.includes("<!DOCTYPE html>") || snippetHtml.includes("<head>") || snippetHtml.includes("<body>")) {
    throw new Error("--snippet contained DOCTYPE or head/body tags");
  }
  if (!snippetHtml.startsWith("<div class=\"vireo-file\"")) {
    throw new Error("--snippet did not start with component root div");
  }

  // 4. Custom title and dark theme
  const customHtml = execSync("./gradlew -q run --args=\"render designs/card.dac --html --title 'Dashboard Preview' --theme dark\"", { encoding: "utf8" });
  if (!customHtml.includes("<title>Dashboard Preview</title>")) {
    throw new Error("Custom title was not included in HTML output");
  }
  if (!customHtml.includes("background-color: #111827;")) {
    throw new Error("Dark theme background was not applied");
  }

  console.log("HTML OPTIONS VERIFIED");
  process.exit(0);
} catch (e) {
  console.error("HTML options verification failed:", e.message);
  process.exit(1);
}
