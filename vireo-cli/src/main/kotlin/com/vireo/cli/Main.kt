package com.vireo.cli

import com.vireo.analysis.Analyzer
import com.vireo.core.VireoError
import com.vireo.core.VireoResult
import com.vireo.parser.Parser
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

    if (args.contains("--help") || args.contains("-h") || args[0] == "help") {
        printUsage(out)
        return 0
    }

    val command = args[0]
    return when (command) {
        "render" -> handleRender(args.drop(1), out, err)
        "check" -> handleCheck(args.drop(1), out, err)
        "init" -> {
            err.println("Error: Command '$command' is not implemented yet.")
            1
        }
        else -> {
            err.println("Error: Unknown command '$command'. Run with --help for usage.")
            1
        }
    }
}

private fun printUsage(out: PrintStream) {
    out.println("Vireo DaaC CLI")
    out.println("Usage: vireo <command> [options]")
    out.println()
    out.println("Commands:")
    out.println("  render <file.dac> [--to json|html] [-o <file>]    Compile and render .dac file")
    out.println("  check <file.dac>                                 Parse and analyze .dac file")
}

private fun handleRender(
    args: List<String>,
    out: PrintStream,
    err: PrintStream
): Int {
    var inputFile: String? = null
    var format = "json"
    var outputFile: String? = null

    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when {
            arg == "--to" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for --to option.")
                    return 1
                }
                format = args[++i]
            }
            arg.startsWith("--to=") -> {
                format = arg.substringAfter("=")
            }
            arg == "-o" || arg == "--output" -> {
                if (i + 1 >= args.size) {
                    err.println("Error: Missing value for $arg option.")
                    return 1
                }
                outputFile = args[++i]
            }
            arg.startsWith("--output=") -> {
                outputFile = arg.substringAfter("=")
            }
            arg.startsWith("-") -> {
                err.println("Error: Unknown option '$arg'.")
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
        err.println("Error: Missing input file. Usage: vireo render <file.dac> [--to json|html] [-o <file>]")
        return 1
    }

    val normalizedFormat = format.lowercase()
    if (normalizedFormat !in setOf("json", "html")) {
        err.println("Error: Unsupported target format '$format'. Supported formats: json, html")
        return 1
    }

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
    val resolvedFile = when (analysisResult) {
        is VireoResult.Err -> {
            printErrors(analysisResult.errors, err)
            return 1
        }
        is VireoResult.Ok -> analysisResult.value
    }

    val renderResult = when (normalizedFormat) {
        "json" -> JsonRenderer.render(resolvedFile)
        "html" -> HtmlRenderer.render(resolvedFile)
        else -> {
            err.println("Error: Unsupported target format '$format'. Supported formats: json, html")
            return 1
        }
    }
    val outputText = when (renderResult) {
        is VireoResult.Err -> {
            printErrors(renderResult.errors, err)
            return 1
        }
        is VireoResult.Ok -> renderResult.value
    }

    if (outputFile != null) {
        try {
            File(outputFile).writeText(outputText)
        } catch (e: Exception) {
            err.println("Error: Failed to write output file '$outputFile': ${e.message}")
            return 1
        }
    } else {
        out.println(outputText)
    }

    return 0
}

private fun handleCheck(
    args: List<String>,
    out: PrintStream,
    err: PrintStream
): Int {
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

private fun printErrors(errors: List<VireoError>, err: PrintStream) {
    for (error in errors) {
        val loc = error.location
        err.println("${loc.file}:${loc.line}:${loc.column}: error: ${error.message}")
    }
}
