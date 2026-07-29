plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":vireo-core"))
    implementation(project(":vireo-lexer"))
    testImplementation(kotlin("test"))
}
