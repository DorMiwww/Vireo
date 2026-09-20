plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":vireo-core"))
    implementation(project(":vireo-analysis"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation(project(":vireo-parser"))
    testImplementation(kotlin("test"))
}
