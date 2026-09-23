package com.vireo.cli

import com.vireo.analysis.Analyzer
import com.vireo.core.VireoError
import com.vireo.core.VireoResult
import com.vireo.parser.Parser
import com.vireo.renderer.figma.FigmaNode
import com.vireo.renderer.figma.FigmaRenderer
import com.vireo.renderer.html.HtmlRenderOptions
import com.vireo.renderer.html.HtmlRenderer
import com.vireo.renderer.json.JsonRenderer
import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = executeCli(args, System.out, System.err)
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

fun executeCli(
    args: Array<String>,
    out: PrintStream = System.out,
    err: PrintStream = System.err
): Int {
    if (args.isEmpty()) {
        printUsage(out)
        return 1
    }

    if (args[0] == "--version" || args[0] == "-v" || args[0] == "version") {
        printVersion(out)
        return 0
    }

    if (args[0] == "help") {
        if (args.size > 1) {
            return when (args[1]) {
                "render" -> { printRenderUsage(out); 0 }
                "check" -> { printCheckUsage(out); 0 }
                "init" -> { printInitUsage(out); 0 }
                "version" -> { printVersion(out); 0 }
                else -> { printUsage(out); 0 }
            }
        }
        printUsage(out)
        return 0
    }

    if (args.contains("--help") || args.contains("-h")) {
        val cmd = args[0]
        return when (cmd) {
            "render" -> { printRenderUsage(out); 0 }
            "check" -> { printCheckUsage(out); 0 }
            "init" -> { printInitUsage(out); 0 }
            "version" -> { printVersion(out); 0 }
            else -> { printUsage(out); 0 }
        }
    }

    val command = args[0]
    val subArgs = args.drop(1)
    return when (command) {
        "render" -> handleRender(subArgs, out, err)
        "check" -> handleCheck(subArgs, out, err)
        "init" -> handleInit(subArgs, out, err)
        "version" -> {
            printVersion(out)
            0
        }
        else -> {
            err.println("Error: Unknown command '$command'. Run with --help for usage.")
            1
        }
    }
}

private fun printVersion(out: PrintStream) {
    out.println("vireo version 0.2.0 (pure JVM 21)")
    out.println("Supported target formats: json, html (standard & snippet), figma")
}

private fun printUsage(out: PrintStream) {
    out.println("Vireo DaaC CLI")
    out.println("Usage: vireo <command> [options]")
    out.println()
    out.println("Commands:")
    out.println("  render <file.dac> [--to json|html|figma] [--token <token>] [-o <file>]    Compile and render .dac file")
    out.println("  check <file.dac>                                                        Parse and analyze .dac file")
    out.println("  init [project-name] [--template <card|minimal|dashboard>] [-d <dir>]     Initialize a new Vireo project (optional, default: vireo-project)")
    out.println("  version                                                                 Print Vireo CLI version and runtime")
    out.println()
    out.println("Global Flags:")
    out.println("  -h, --help       Show help for vireo or any subcommand")
    out.println("  -v, --version    Show Vireo version")
    out.println()
    out.println("Use 'vireo <command> --help' or 'vireo help <command>' for detailed command options.")
}

private fun printRenderUsage(out: PrintStream) {
    out.println("Vireo DaaC CLI — render")
    out.println()
    out.println("Compiles and renders a .dac design file to the target format (JSON, HTML, or Figma).")
    out.println()
    out.println("Usage:")
    out.println("  vireo render <file.dac> [options]")
    out.println()
    out.println("Format Options (kubectl-style):")
    out.println("  -o, --output <format>        Target format: json, html, figma, standard-html (default: json)")
    out.println("  --to <format>                Target format alias: json, html, figma")
    out.println("  --html                       Shorthand for --output html")
    out.println("  --figma                      Shorthand for --output figma")
    out.println("  --json                       Shorthand for --output json")
    out.println()
    out.println("Output Destination:")
    out.println("  --out, --output-file <file>  Explicit file path to write output to")
    out.println("  -o <file>                    Explicit file path to write output to")
    out.println("  --stdout, -o -               Print output directly to terminal stdout (without creating a file)")
    out.println("  (default)                    Automatically creates <file>.<ext> (.html, .figma.json, .json)")
    out.println()
    out.println("HTML Output Parameters:")
    out.println("  --standard-html, --full-page Complete HTML5 document with <!DOCTYPE html>, head, title (default: true)")
    out.println("  --snippet, --fragment        Component fragment only (no <!DOCTYPE>, <html>, <body> wrapper)")
    out.println("  --title <title>              Custom HTML document title (defaults to file path)")
    out.println("  --theme <light|dark>         Preview background theme (default: light)")
    out.println()
    out.println("Figma Output Parameters:")
    out.println("  --token <token>              Figma Personal Access Token (or FIGMA_TOKEN environment variable)")
    out.println("  --pretty                     Format Figma JSON with pretty indentation (default: true)")
    out.println("  --compact                    Output compact/minified Figma JSON")
    out.println()
    out.println("Asset Pipeline Parameters:")
    out.println("  --embed-assets               Embed local images and assets as base64 data URIs in HTML and Figma JSON (default: false)")
    out.println()
    out.println("Examples:")
    out.println("  # Render design to card.html automatically")
    out.println("  vireo render designs/card.dac --html")
    out.println()
    out.println("  # Render design to card.figma.json automatically")
    out.println("  vireo render designs/card.dac --figma")
    out.println()
    out.println("  # Render design to card.json automatically")
    out.println("  vireo render designs/card.dac -o json")
    out.println()
    out.println("  # Output directly to terminal stdout")
    out.println("  vireo render designs/card.dac --html --stdout")
    out.println("  vireo render designs/card.dac -o -")
    out.println()
    out.println("  # Render standard HTML with custom title and output file")
    out.println("  vireo render designs/card.dac --html --title \"Dashboard\" --out dashboard.html")
    out.println()
    out.println("  # Render HTML component fragment/snippet")
    out.println("  vireo render designs/card.dac --html --snippet")
}

private fun printCheckUsage(out: PrintStream) {
    out.println("Vireo DaaC CLI — check")
    out.println()
    out.println("Parses and analyzes a .dac design file (validating syntax, loading imports,")
    out.println("resolving cross-file references, and checking layout constraints).")
    out.println()
    out.println("Usage:")
    out.println("  vireo check <file.dac>")
    out.println()
    out.println("Output:")
    out.println("  - Prints 'OK: <file.dac> passed analysis.' with exit code 0 on success.")
    out.println("  - Prints all errors in '<file>:<line>:<column>: error: <message>' format with exit code 1 on failure.")
    out.println()
    out.println("Examples:")
    out.println("  vireo check designs/card.dac")
    out.println("  vireo check designs/tokens.dac")
}

private fun printInitUsage(out: PrintStream) {
    out.println("Vireo DaaC CLI — init (Phase 5)")
    out.println()
    out.println("Scaffolds a new Vireo DaaC project with recommended structure, design tokens,")
    out.println("reusable components, and a runnable example card.")
    out.println()
    out.println("Usage:")
    out.println("  vireo init [project-name] [options]")
    out.println()
    out.println("Arguments:")
    out.println("  [project-name]               Name of the project (optional, default: vireo-project). Use '.' for current directory.")
    out.println()
    out.println("Options:")
    out.println("  -n, --name <name>            Explicit project name (optional, default: vireo-project)")
    out.println("  --template <template>        Template preset: card, minimal, dashboard (default: card)")
    out.println("  -d, --dir <directory>        Target directory to scaffold into (default: ./[project-name])")
    out.println("  --force                      Overwrite existing files in target directory")
    out.println("  -h, --help                   Show this help message")
    out.println()
    out.println("Examples:")
    out.println("  # Initialize with default project name (vireo-project)")
    out.println("  vireo init")
    out.println()
    out.println("  # Initialize with custom project name")
    out.println("  vireo init my-design-system")
    out.println()
    out.println("  # Initialize directly in current directory")
    out.println("  vireo init .")
    out.println("  # or: vireo init --dir .")
}

private fun handleRender(
    args: List<String>,
    out: PrintStream,
    err: PrintStream
): Int {
    if (args.contains("--help") || args.contains("-h")) {
        printRenderUsage(out)
        return 0
    }

    var inputFile: String? = null
    var format = "json"
    var formatSpecifiedExplicitly = false
    var outputFile: String? = null
    var printToStdout = false
    var token: String? = System.getenv("FIGMA_TOKEN")
    var standardHtml = true
    var htmlTitle: String? = null
    var htmlTheme = "light"
    var prettyFigma = true
    var embedAssets = false

    val recognizedFormats = setOf("json", "html", "figma", "standard-html")

    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg == "--stdout" || arg == "--console" || arg == "--terminal" -> {
                printToStdout = true
            }
            arg == "--to" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --to option.")
                    return 1
                }
                format = args[++i]
                formatSpecifiedExplicitly = true
            }
            arg.startsWith("--to=") -> {
                format = arg.substringAfter("=")
                formatSpecifiedExplicitly = true
            }
            arg == "--output" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --output option.")
                    return 1
                }
                val nextVal = args[++i]
                val lower = nextVal.lowercase()
                if (nextVal == "-") {
                    printToStdout = true
                } else if (lower in recognizedFormats) {
                    format = lower
                    formatSpecifiedExplicitly = true
                } else {
                    outputFile = nextVal
                }
            }
            arg.startsWith("--output=") -> {
                val value = arg.substringAfter("=")
                val lower = value.lowercase()
                if (value == "-") {
                    printToStdout = true
                } else if (lower in recognizedFormats) {
                    format = lower
                    formatSpecifiedExplicitly = true
                } else {
                    outputFile = value
                }
            }
            arg == "-o" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for -o option.")
                    return 1
                }
                val nextVal = args[++i]
                val lower = nextVal.lowercase()
                if (nextVal == "-") {
                    printToStdout = true
                } else if (lower in recognizedFormats && !formatSpecifiedExplicitly) {
                    format = lower
                    formatSpecifiedExplicitly = true
                } else {
                    outputFile = nextVal
                }
            }
            arg.startsWith("-o=") -> {
                val value = arg.substringAfter("=")
                val lower = value.lowercase()
                if (value == "-") {
                    printToStdout = true
                } else if (lower in recognizedFormats && !formatSpecifiedExplicitly) {
                    format = lower
                    formatSpecifiedExplicitly = true
                } else {
                    outputFile = value
                }
            }
            arg == "--out" || arg == "--output-file" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for $arg option.")
                    return 1
                }
                val nextVal = args[++i]
                if (nextVal == "-") {
                    printToStdout = true
                } else {
                    outputFile = nextVal
                }
            }
            arg.startsWith("--out=") -> {
                val value = arg.substringAfter("=")
                if (value == "-") {
                    printToStdout = true
                } else {
                    outputFile = value
                }
            }
            arg.startsWith("--output-file=") -> {
                val value = arg.substringAfter("=")
                if (value == "-") {
                    printToStdout = true
                } else {
                    outputFile = value
                }
            }
            arg == "--html" -> {
                format = "html"
                formatSpecifiedExplicitly = true
            }
            arg == "--figma" -> {
                format = "figma"
                formatSpecifiedExplicitly = true
            }
            arg == "--json" -> {
                format = "json"
                formatSpecifiedExplicitly = true
            }
            arg == "--standard-html" || arg == "--full-page" -> {
                format = "html"
                standardHtml = true
                formatSpecifiedExplicitly = true
            }
            arg == "--snippet" || arg == "--fragment" -> {
                format = "html"
                standardHtml = false
                formatSpecifiedExplicitly = true
            }
            arg == "--title" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --title option.")
                    return 1
                }
                htmlTitle = args[++i]
            }
            arg.startsWith("--title=") -> {
                htmlTitle = arg.substringAfter("=")
            }
            arg == "--theme" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --theme option.")
                    return 1
                }
                htmlTheme = args[++i]
            }
            arg.startsWith("--theme=") -> {
                htmlTheme = arg.substringAfter("=")
            }
            arg == "--token" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --token option.")
                    return 1
                }
                token = args[++i]
            }
            arg.startsWith("--token=") -> {
                token = arg.substringAfter("=")
            }
            arg == "--pretty" -> {
                prettyFigma = true
            }
            arg == "--compact" -> {
                prettyFigma = false
            }
            arg == "--embed-assets" -> {
                embedAssets = true
            }
            arg.startsWith("-") -> {
                err.println("Error: Unknown option '$arg'. Run with --help for usage.")
                return 1
            }
            else -> {
                if (inputFile == null) {
                    inputFile = arg
                } else {
                    err.println("Error: Unexpected extra argument '$arg'.")
                    return 1
                }
            }
        }
        i++
    }

    if (inputFile == null) {
        err.println("Error: Missing input file. Usage: vireo render <file.dac> [--to json|html|figma] [--token <token>] [-o <file>]")
        return 1
    }

    val normalizedFormat = when (format.lowercase()) {
        "standard-html" -> "html"
        else -> format.lowercase()
    }
    if (normalizedFormat !in setOf("json", "html", "figma")) {
        err.println("Error: Unsupported target format '$format'. Supported formats: json, html, figma")
        return 1
    }

    val file = File(inputFile)
    if (!file.exists() || !file.isFile) {
        err.println("Error: Input file '$inputFile' does not exist or is not a readable file.")
        return 1
    }

    val sourceText = try {
        file.readText(Charsets.UTF_8)
    } catch (e: Exception) {
        err.println("Error: Failed to read file '$inputFile': ${e.message}")
        return 1
    }

    val parseResult = Parser.parse(sourceText, inputFile)
    val vireoFile = when (parseResult) {
        is VireoResult.Err -> {
            printErrors(parseResult.errors, err)
            return 1
        }
        is VireoResult.Ok -> parseResult.value
    }

    val analysisResult = Analyzer.analyze(vireoFile)
    val resolvedFile = when (analysisResult) {
        is VireoResult.Err -> {
            printErrors(analysisResult.errors, err)
            return 1
        }
        is VireoResult.Ok -> analysisResult.value
    }

    val outputText = when (normalizedFormat) {
        "json" -> {
            when (val r = JsonRenderer.render(resolvedFile)) {
                is VireoResult.Err -> { printErrors(r.errors, err); return 1 }
                is VireoResult.Ok -> r.value
            }
        }
        "html" -> {
            val inputDir = File(inputFile).parentFile ?: File(".")
            val assetResolver: (String) -> String = if (embedAssets) {
                { assetPath ->
                    val trimmed = assetPath.trim('"', '\'')
                    if (trimmed.startsWith("data:") || trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("//")) {
                        trimmed
                    } else {
                        val assetFile = File(inputDir, trimmed)
                        if (assetFile.exists() && assetFile.isFile) {
                            val ext = assetFile.extension.lowercase()
                            val mime = when (ext) {
                                "png" -> "image/png"
                                "jpg", "jpeg" -> "image/jpeg"
                                "webp" -> "image/webp"
                                "gif" -> "image/gif"
                                "avif" -> "image/avif"
                                "svg" -> "image/svg+xml"
                                "mp4" -> "video/mp4"
                                "webm" -> "video/webm"
                                "mp3" -> "audio/mpeg"
                                "wav" -> "audio/wav"
                                "ogg" -> "audio/ogg"
                                else -> "application/octet-stream"
                            }
                            val base64 = java.util.Base64.getEncoder().encodeToString(assetFile.readBytes())
                            "data:$mime;base64,$base64"
                        } else {
                            trimmed
                        }
                    }
                }
            } else {
                { it }
            }
            val htmlOptions = HtmlRenderOptions(
                standardHtml = standardHtml,
                title = htmlTitle,
                theme = htmlTheme,
                assetResolver = assetResolver
            )
            when (val r = HtmlRenderer.render(resolvedFile, htmlOptions)) {
                is VireoResult.Err -> { printErrors(r.errors, err); return 1 }
                is VireoResult.Ok -> r.value
            }
        }
        "figma" -> {
            when (val r = FigmaRenderer.render(resolvedFile)) {
                is VireoResult.Err -> { printErrors(r.errors, err); return 1 }
                is VireoResult.Ok -> {
                    val doc = r.value
                    val finalDoc = if (embedAssets) {
                        val inputDir = File(inputFile).parentFile ?: File(".")
                        doc.copy(nodes = doc.nodes.map { embedFigmaNodeAssets(it, inputDir) })
                    } else {
                        doc
                    }
                    finalDoc.toJson(pretty = prettyFigma)
                }
            }
        }
        else -> {
            err.println("Error: Unsupported target format '$format'. Supported formats: json, html, figma")
            return 1
        }
    }

    if (normalizedFormat == "figma" && token != null) {
        val httpResult = FigmaApiTransport.postDocument(token, outputText)
        httpResult.onSuccess {
            out.println("Figma API: Successfully posted design to Figma.")
        }.onFailure { ex ->
            err.println("Figma API Error: ${ex.message}")
        }
    }

    if (printToStdout || outputFile == "-") {
        out.println(outputText)
        return 0
    }

    val targetFile = if (outputFile != null) {
        outputFile
    } else {
        val baseName = File(inputFile).nameWithoutExtension
        when (normalizedFormat) {
            "html" -> "$baseName.html"
            "figma" -> "$baseName.figma.json"
            "json" -> "$baseName.json"
            else -> "$baseName.$normalizedFormat"
        }
    }

    try {
        val destFile = File(targetFile)
        destFile.parentFile?.mkdirs()
        destFile.writeText(outputText, Charsets.UTF_8)
    } catch (e: Exception) {
        err.println("Error: Failed to write output file '$targetFile': ${e.message}")
        return 1
    }

    val formatDisplay = when (normalizedFormat) {
        "html" -> "HTML"
        "figma" -> "Figma"
        "json" -> "JSON"
        else -> normalizedFormat.uppercase()
    }
    out.println("✨ Rendered $formatDisplay to $targetFile")

    return 0
}

private fun handleCheck(
    args: List<String>,
    out: PrintStream,
    err: PrintStream
): Int {
    if (args.contains("--help") || args.contains("-h")) {
        printCheckUsage(out)
        return 0
    }

    if (args.isEmpty()) {
        err.println("Error: Missing input file. Usage: vireo check <file.dac>")
        return 1
    }

    val inputFile = args[0]
    val file = File(inputFile)
    if (!file.exists() || !file.isFile) {
        err.println("Error: Input file '$inputFile' does not exist or is not a readable file.")
        return 1
    }

    val sourceText = try {
        file.readText()
    } catch (e: Exception) {
        err.println("Error: Failed to read file '$inputFile': ${e.message}")
        return 1
    }

    val parseResult = Parser.parse(sourceText, inputFile)
    val vireoFile = when (parseResult) {
        is VireoResult.Err -> {
            printErrors(parseResult.errors, err)
            return 1
        }
        is VireoResult.Ok -> parseResult.value
    }

    val analysisResult = Analyzer.analyze(vireoFile)
    return when (analysisResult) {
        is VireoResult.Err -> {
            printErrors(analysisResult.errors, err)
            1
        }
        is VireoResult.Ok -> {
            out.println("OK: $inputFile passed analysis.")
            0
        }
    }
}

private fun handleInit(
    args: List<String>,
    out: PrintStream,
    err: PrintStream
): Int {
    if (args.contains("--help") || args.contains("-h")) {
        printInitUsage(out)
        return 0
    }

    var projectName: String? = null
    var template = "card"
    var targetDirPath: String? = null
    var force = false

    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg == "-n" || arg == "--name" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for $arg option.")
                    return 1
                }
                projectName = args[++i]
            }
            arg.startsWith("--name=") -> {
                projectName = arg.substringAfter("=")
            }
            arg == "--template" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --template option.")
                    return 1
                }
                template = args[++i]
            }
            arg.startsWith("--template=") -> {
                template = arg.substringAfter("=")
            }
            arg == "-d" || arg == "--dir" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for $arg option.")
                    return 1
                }
                targetDirPath = args[++i]
            }
            arg.startsWith("--dir=") -> {
                targetDirPath = arg.substringAfter("=")
            }
            arg == "--force" -> {
                force = true
            }
            arg.startsWith("-") -> {
                err.println("Error: Unknown option '$arg'. Run 'vireo init --help' for usage.")
                return 1
            }
            else -> {
                if (projectName == null) {
                    projectName = arg
                } else {
                    projectName = "$projectName-$arg"
                }
            }
        }
        i++
    }

    val isCurrentDir = projectName == "." || targetDirPath == "." || targetDirPath == "./"
    val effectiveProjectName = when {
        projectName != null && projectName != "." -> projectName
        isCurrentDir -> {
            try {
                File(".").canonicalFile.name.ifEmpty { "vireo-project" }
            } catch (e: Exception) {
                "vireo-project"
            }
        }
        else -> "vireo-project"
    }

    val targetDir = when {
        targetDirPath != null -> File(targetDirPath)
        projectName == "." -> File(".")
        else -> File(effectiveProjectName)
    }

    val configJson = """
        {
          "name": "$effectiveProjectName",
          "version": "0.2.0",
          "template": "$template",
          "defaultRenderer": "html",
          "entry": "designs/card.dac"
        }
    """.trimIndent()

    val tokensDac = """
        var primaryColor = #3B82F6
        var textPrimary  = #111827
        var textMuted    = #6B7280
        var bgLight      = #FFFFFF
        var radiusBase   = 12
        var spacingBase  = 16

        block Tokens {
            component Theme {
                color: ${'$'}primaryColor
                radius: ${'$'}radiusBase
            }
        }
    """.trimIndent()

    val buttonDac = """
        import tokens from "../tokens.dac"

        block Primary {
            component Default {
                layout: horizontal
                alignItems: center
                justifyContent: center
                width: 140
                height: 44
                color: #3B82F6
                radius: 8

                component Label {
                    text: "Get Started"
                    fontSize: 14
                    fontWeight: bold
                    color: #FFFFFF
                }
            }
        }
    """.trimIndent()

    val cardDac = """
        import tokens from "./tokens.dac"
        import buttons from "./components/button.dac"

        block Cards {
            component Card {
                layout: vertical
                mainAxis: hug
                crossAxis: fill
                gap: 16
                padding: 24
                width: 380
                color: #FFFFFF
                radius: 12
                shadow: true

                component Title {
                    text: "Welcome to Vireo"
                    fontSize: 22
                    fontWeight: bold
                    color: #111827
                }

                component Description {
                    text: "Vireo is a Design as a Code (DaaC) platform. Describe UI in code, render anywhere."
                    fontSize: 14
                    color: #6B7280
                }

                component ActionButton {
                    ref: buttons.Primary.Default
                    label: "Open Docs"
                    href: "https://github.com/DorMiwww/Vireo"
                    width: fill
                }
            }
        }
    """.trimIndent()

    val readmeMd = """
        # $effectiveProjectName

        Design as a Code project created with Vireo.

        ## Structure

        - `vireo.config.json` — Project configuration
        - `designs/tokens.dac` — Design tokens (colors, typography, spacing)
        - `designs/components/button.dac` — Button component library
        - `designs/card.dac` — Main screen composing components

        ## Commands

        ```bash
        # Validate syntax and references
        vireo check designs/card.dac

        # Render to standard HTML5 document (creates card.html)
        vireo render designs/card.dac --html

        # Render to Figma AST JSON (creates card.figma.json)
        vireo render designs/card.dac --figma
        ```
    """.trimIndent()

    val filesToCreate = listOf(
        File(targetDir, "vireo.config.json") to configJson,
        File(targetDir, "designs/tokens.dac") to tokensDac,
        File(targetDir, "designs/components/button.dac") to buttonDac,
        File(targetDir, "designs/card.dac") to cardDac,
        File(targetDir, "README.md") to readmeMd
    )

    if (targetDir.exists() && !force) {
        val conflicts = filesToCreate.filter { it.first.exists() }
        if (conflicts.isNotEmpty()) {
            err.println("Error: Directory '${targetDir.path}' already contains Vireo files (${conflicts.first().first.name}). Use --force to overwrite.")
            return 1
        }
    }

    try {
        targetDir.mkdirs()
        for ((f, content) in filesToCreate) {
            f.parentFile?.mkdirs()
            f.writeText(content, Charsets.UTF_8)
        }
    } catch (e: Exception) {
        err.println("Error: Failed to initialize project: ${e.message}")
        return 1
    }

    out.println("✨ Successfully initialized Vireo project '$effectiveProjectName'!")
    out.println()
    out.println("Project files created:")
    out.println("  - ${File(targetDir, "vireo.config.json").path}")
    out.println("  - ${File(targetDir, "designs/tokens.dac").path}")
    out.println("  - ${File(targetDir, "designs/components/button.dac").path}")
    out.println("  - ${File(targetDir, "designs/card.dac").path}")
    out.println("  - ${File(targetDir, "README.md").path}")
    out.println()
    out.println("Next steps:")
    if (targetDir.path != "." && targetDir.path != "./") {
        out.println("  1. cd ${targetDir.path}")
        out.println("  2. vireo check designs/card.dac")
        out.println("  3. vireo render designs/card.dac --html")
    } else {
        out.println("  1. vireo check designs/card.dac")
        out.println("  2. vireo render designs/card.dac --html")
    }

    return 0
}

private fun printErrors(errors: List<VireoError>, err: PrintStream) {
    for (error in errors) {
        val loc = error.location
        err.println("${loc.file}:${loc.line}:${loc.column}: error: ${error.message}")
    }
}

private fun embedFigmaNodeAssets(node: FigmaNode, baseDir: File): FigmaNode {
    var newImgBase64 = node.imageBase64
    var newSvgContent = node.svgContent
    val mediaUrl = node.mediaUrl
    if (mediaUrl != null && !mediaUrl.startsWith("http://") && !mediaUrl.startsWith("https://") && !mediaUrl.startsWith("data:")) {
        val f = File(baseDir, mediaUrl)
        if (f.exists() && f.isFile) {
            if (node.mediaType == "SVG" || f.extension.equals("svg", ignoreCase = true)) {
                newSvgContent = f.readText(Charsets.UTF_8)
            } else {
                newImgBase64 = java.util.Base64.getEncoder().encodeToString(f.readBytes())
            }
        }
    }
    val newFills = node.fills?.map { paint ->
        val ref = paint.imageRef
        if (paint.type == "IMAGE" && ref != null && !ref.startsWith("http://") && !ref.startsWith("https://") && !ref.startsWith("data:")) {
            val f = File(baseDir, ref)
            if (f.exists() && f.isFile) {
                val mime = when (f.extension.lowercase()) {
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "avif" -> "image/avif"
                    "svg" -> "image/svg+xml"
                    else -> "application/octet-stream"
                }
                val b64 = java.util.Base64.getEncoder().encodeToString(f.readBytes())
                paint.copy(imageRef = "data:$mime;base64,$b64")
            } else {
                paint
            }
        } else {
            paint
        }
    }
    val newChildren = node.children?.map { embedFigmaNodeAssets(it, baseDir) }
    return node.copy(
        imageBase64 = newImgBase64,
        svgContent = newSvgContent,
        fills = newFills,
        children = newChildren
    )
}

