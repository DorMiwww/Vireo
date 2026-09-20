import java.nio.file.Files

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
    applicationName = "vireo"
}

tasks.named<Tar>("distTar") {
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.register("installCli") {
    dependsOn("installDist")
    description = "Installs the vireo CLI executable to ~/.local/bin/vireo"
    group = "distribution"
    doLast {
        val userLocalBin = File(System.getProperty("user.home"), ".local/bin")
        userLocalBin.mkdirs()
        val binary = File(layout.buildDirectory.get().asFile, "install/vireo/bin/vireo")
        val link = File(userLocalBin, "vireo")
        if (link.exists() || Files.isSymbolicLink(link.toPath())) {
            link.delete()
        }
        Files.createSymbolicLink(link.toPath(), binary.toPath())
        println("Successfully installed vireo to ${link.absolutePath}")
    }
}
