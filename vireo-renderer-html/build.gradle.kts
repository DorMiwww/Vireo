plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":vireo-core"))
    implementation(project(":vireo-analysis"))
    testImplementation(project(":vireo-parser"))
    testImplementation(kotlin("test"))
}
