plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":vireo-core"))
    implementation(project(":vireo-analysis"))
    testImplementation(kotlin("test"))
}
