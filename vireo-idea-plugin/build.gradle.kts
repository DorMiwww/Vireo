plugins {
    kotlin("jvm") version "2.1.0"
    // No version here: the settings plugin (settings.gradle.kts) already puts
    // this plugin on the classpath at 2.19.0 — a second version here conflicts.
    id("org.jetbrains.intellij.platform")
}

group = "com.vireo"
version = "0.2.0-SNAPSHOT"

// No repositories {} block here: settings.gradle.kts declares
// RepositoriesMode.FAIL_ON_PROJECT_REPOS, so all repositories come from its
// dependencyResolutionManagement block instead.

dependencies {
    intellijPlatform {
        intellijIdea("2024.3")
    }

    // Built jars from vireo-core / vireo-lexer / vireo-parser / vireo-analysis.
    // Plain file dependencies on purpose — see BLOCK.md: this project runs on
    // Gradle 9 / Kotlin 2.x, incompatible with the root build's Gradle 8.5 /
    // Kotlin 1.9.22, so it cannot use live project(...) dependencies.
    // Regenerate with: (from repo root)
    //   ./gradlew :vireo-core:jar :vireo-lexer:jar :vireo-parser:jar :vireo-analysis:jar
    // then copy the four jars from */build/libs/ into vireo-idea-plugin/libs/
    implementation(fileTree("libs") { include("*.jar") })
}

kotlin {
    jvmToolchain(17)
}

tasks {
    wrapper {
        gradleVersion = "9.0.0"
    }
}
