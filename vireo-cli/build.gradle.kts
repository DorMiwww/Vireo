plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation(project(":vireo-core"))
    implementation(project(":vireo-lexer"))
    implementation(project(":vireo-parser"))
    implementation(project(":vireo-analysis"))
    implementation(project(":vireo-renderer-json"))
    implementation(project(":vireo-renderer-html"))
    implementation(project(":vireo-renderer-figma"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.vireo.cli.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
