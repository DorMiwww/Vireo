import { execSync } from "child_process";

try {
  const rootHelp = execSync("./gradlew -q run --args=\"--help\"", { encoding: "utf8" });
  if (!rootHelp.includes("Vireo DaaC CLI") || !rootHelp.includes("Usage: vireo <command>")) {
    throw new Error("Root help output missing expected text");
  }

  const renderHelp = execSync("./gradlew -q run --args=\"render --help\"", { encoding: "utf8" });
  if (!renderHelp.includes("Vireo DaaC CLI — render")) {
    throw new Error("Render help missing header");
  }
  if (!renderHelp.includes("-o, --output <format>")) {
    throw new Error("Render help missing -o, --output option");
  }
  if (!renderHelp.includes("--html") || !renderHelp.includes("--figma")) {
    throw new Error("Render help missing shorthand flags");
  }
  if (!renderHelp.includes("--standard-html") || !renderHelp.includes("--snippet")) {
    throw new Error("Render help missing HTML parameters");
  }
  if (!renderHelp.includes("Examples:")) {
    throw new Error("Render help missing Examples section");
  }

  const checkHelp = execSync("./gradlew -q run --args=\"check --help\"", { encoding: "utf8" });
  if (!checkHelp.includes("Vireo DaaC CLI — check")) {
    throw new Error("Check help missing header");
  }

  const initHelp = execSync("./gradlew -q run --args=\"init --help\"", { encoding: "utf8" });
  if (!initHelp.includes("Vireo DaaC CLI — init")) {
    throw new Error("Init help missing header");
  }

  console.log("CLI HELP VERIFIED");
  process.exit(0);
} catch (e) {
  console.error("CLI help verification failed:", e.message);
  process.exit(1);
}
