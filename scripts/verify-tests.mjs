import { execSync } from "child_process";

try {
  console.log("Running ./gradlew test...");
  execSync("./gradlew test", { stdio: "inherit" });
  console.log("ALL GRADLE TESTS PASSED");
  process.exit(0);
} catch (e) {
  console.error("Test execution failed:", e.message);
  process.exit(1);
}
