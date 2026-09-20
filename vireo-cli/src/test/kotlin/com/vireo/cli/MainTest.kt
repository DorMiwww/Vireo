package com.vireo.cli

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {

    private fun runCliTest(vararg args: String): Triplet {
        val outStream = ByteArrayOutputStream()
        val errStream = ByteArrayOutputStream()
        val outPrint = PrintStream(outStream)
        val errPrint = PrintStream(errStream)

        val exitCode = executeCli(args.toList().toTypedArray(), outPrint, errPrint)
        outPrint.flush()
        errPrint.flush()

        return Triplet(exitCode, outStream.toString(), errStream.toString())
    }

    private data class Triplet(val exitCode: Int, val stdout: String, val stderr: String)

    @Test
    fun `test render command with valid dac file prints json to stdout`() {
        val tempFile = File.createTempFile("hello", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: "#3B82F6"
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest("render", tempFile.absolutePath, "--to", "json")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("\"name\": \"Main\""))
        assertTrue(stdout.contains("\"name\": \"Box\""))
        assertTrue(stdout.contains("\"width\": 200"))
        assertTrue(stdout.contains("\"color\": \"#3B82F6\""))
    }

    @Test
    fun `test render command with output file flag writes to file`() {
        val inputFile = File.createTempFile("button", ".dac")
        inputFile.deleteOnExit()
        inputFile.writeText(
            """
            block Primary {
                component Default {
                    width: 120
                    height: 40
                }
            }
            """.trimIndent()
        )

        val outputFile = File.createTempFile("output", ".json")
        outputFile.deleteOnExit()

        val (exitCode, stdout, stderr) = runCliTest("render", inputFile.absolutePath, "--to=json", "-o", outputFile.absolutePath)

        assertEquals(0, exitCode)
        assertTrue(stdout.isEmpty())
        assertTrue(stderr.isEmpty())

        val writtenJson = outputFile.readText()
        assertTrue(writtenJson.contains("\"name\": \"Primary\""))
        assertTrue(writtenJson.contains("\"name\": \"Default\""))
        assertTrue(writtenJson.contains("\"width\": 120"))
    }

    @Test
    fun `test render with syntax error prints formatted error and returns 1`() {
        val tempFile = File.createTempFile("invalid", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Box {
                    width
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest("render", tempFile.absolutePath, "--to", "json")

        assertEquals(1, exitCode)
        assertTrue(stdout.isEmpty())
        assertTrue(stderr.contains(": error: "))
    }

    @Test
    fun `test render with non existent file returns exit code 1`() {
        val (exitCode, stdout, stderr) = runCliTest("render", "non_existent_file.dac", "--to", "json")

        assertEquals(1, exitCode)
        assertTrue(stdout.isEmpty())
        assertTrue(stderr.contains("Error: Input file 'non_existent_file.dac' does not exist"))
    }

    @Test
    fun `test render with unsupported format returns exit code 1`() {
        val tempFile = File.createTempFile("test", ".dac")
        tempFile.deleteOnExit()

        val (exitCode, stdout, stderr) = runCliTest("render", tempFile.absolutePath, "--to", "xml")

        assertEquals(1, exitCode)
        assertTrue(stdout.isEmpty())
        assertTrue(stderr.contains("Error: Unsupported target format 'xml'"))
        assertTrue(stderr.contains("Supported formats: json, html"))
    }

    @Test
    fun `test render command with html format prints html to stdout`() {
        val tempFile = File.createTempFile("card", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Cards {
                component Card {
                    width: 200
                    height: 100
                    color: "#3B82F6"
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest("render", tempFile.absolutePath, "--to", "html")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("class=\"vireo-file\""))
        assertTrue(stdout.contains("data-name=\"Card\""))
        assertTrue(stdout.contains("width: 200px;"))
    }

    @Test
    fun `test check command reports syntax error with file line col format`() {
        val tempFile = File.createTempFile("bad_syntax", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Box {
                    width
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest("check", tempFile.absolutePath)

        assertEquals(1, exitCode)
        assertTrue(stdout.isEmpty())
        assertTrue(stderr.contains("${tempFile.absolutePath}:3:"))
        assertTrue(stderr.contains(": error: "))
    }

    @Test
    fun `test check command with missing input file returns exit code 1`() {
        val (exitCodeCheck, _, stderrCheck) = runCliTest("check")
        assertEquals(1, exitCodeCheck)
        assertTrue(stderrCheck.contains("Error: Missing input file. Usage: vireo check <file.dac>"))
    }

    @Test
    fun `test help flag returns exit code 0 and prints usage`() {
        val (exitCode, stdout, stderr) = runCliTest("--help")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("Vireo DaaC CLI"))
        assertTrue(stdout.contains("Usage: vireo <command>"))
    }

    @Test
    fun `test render help flag returns exit code 0 and prints render usage with kubectl options`() {
        val (exitCode, stdout, stderr) = runCliTest("render", "--help")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("Vireo DaaC CLI — render"))
        assertTrue(stdout.contains("Format Options (kubectl-style):"))
        assertTrue(stdout.contains("-o, --output <format>"))
        assertTrue(stdout.contains("--html"))
        assertTrue(stdout.contains("--figma"))
        assertTrue(stdout.contains("--standard-html"))
        assertTrue(stdout.contains("--snippet"))
        assertTrue(stdout.contains("Examples:"))
    }

    @Test
    fun `test help render subcommand prints render usage`() {
        val (exitCode, stdout, stderr) = runCliTest("help", "render")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("Vireo DaaC CLI — render"))
        assertTrue(stdout.contains("Format Options (kubectl-style):"))
    }

    @Test
    fun `test check help flag returns exit code 0 and prints check usage`() {
        val (exitCode, stdout, stderr) = runCliTest("check", "--help")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("Vireo DaaC CLI — check"))
        assertTrue(stdout.contains("Usage:"))
        assertTrue(stdout.contains("vireo check <file.dac>"))
    }

    @Test
    fun `test init help flag returns exit code 0 and prints init usage`() {
        val (exitCode, stdout, stderr) = runCliTest("init", "--help")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("Vireo DaaC CLI — init"))
        assertTrue(stdout.contains("vireo init [project-name]"))
    }

    @Test
    fun `test version command and flag print version info`() {
        val (exitCodeCmd, stdoutCmd, stderrCmd) = runCliTest("version")
        assertEquals(0, exitCodeCmd)
        assertTrue(stderrCmd.isEmpty())
        assertTrue(stdoutCmd.contains("vireo version 0.1.0-SNAPSHOT"))
        assertTrue(stdoutCmd.contains("pure JVM 21"))

        val (exitCodeFlag, stdoutFlag, stderrFlag) = runCliTest("--version")
        assertEquals(0, exitCodeFlag)
        assertTrue(stderrFlag.isEmpty())
        assertTrue(stdoutFlag.contains("vireo version 0.1.0-SNAPSHOT"))
    }

    @Test
    fun `test render with kubectl style -o html flag prints standard HTML`() {
        val tempFile = File.createTempFile("kubectl_html", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Title {
                    text: "Hello Kubectl"
                    fontSize: 20
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest("render", tempFile.absolutePath, "-o", "html")
        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.startsWith("<!DOCTYPE html>"))
        assertTrue(stdout.contains("Hello Kubectl"))
    }

    @Test
    fun `test render with shorthand --html flag and custom title and dark theme`() {
        val tempFile = File.createTempFile("title_theme", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Box {
                    width: 100
                    height: 100
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest(
            "render", tempFile.absolutePath,
            "--html",
            "--title=Dark Dashboard",
            "--theme=dark"
        )
        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.startsWith("<!DOCTYPE html>"))
        assertTrue(stdout.contains("<title>Dark Dashboard</title>"))
        assertTrue(stdout.contains("background-color: #111827;"))
    }

    @Test
    fun `test render with snippet flag omits DOCTYPE and renders component fragment`() {
        val tempFile = File.createTempFile("snippet", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Box {
                    width: 100
                    height: 100
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest(
            "render", tempFile.absolutePath,
            "--html",
            "--snippet"
        )
        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(!stdout.contains("<!DOCTYPE html>"))
        assertTrue(!stdout.contains("<head>"))
        assertTrue(stdout.startsWith("<div class=\"vireo-file\""))
        assertTrue(stdout.contains("data-name=\"Box\""))
    }

    @Test
    fun `test render with figma flag and compact option`() {
        val tempFile = File.createTempFile("figma_test", ".dac")
        tempFile.deleteOnExit()
        tempFile.writeText(
            """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: #3B82F6
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCliTest(
            "render", tempFile.absolutePath,
            "--figma",
            "--compact"
        )
        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("\"type\":\"CANVAS\""))
        assertTrue(stdout.contains("\"name\":\"Main\""))
        // Compact JSON has no multiline indentation
        assertTrue(!stdout.contains("{\n  "))
    }

    @Test
    fun `test init command scaffolds a new project with dac components and config`() {
        val tempDir = File.createTempFile("init_test_proj_", "")
        tempDir.delete() // convert to directory path
        tempDir.deleteOnExit()

        val (exitCode, stdout, stderr) = runCliTest("init", "DemoApp", "-d", tempDir.absolutePath)
        assertEquals(0, exitCode, "init command failed: $stderr")
        assertTrue(stderr.isEmpty())
        assertTrue(stdout.contains("Successfully initialized Vireo project 'DemoApp'"))

        val configFile = File(tempDir, "vireo.config.json")
        val tokensFile = File(tempDir, "designs/tokens.dac")
        val buttonFile = File(tempDir, "designs/components/button.dac")
        val cardFile = File(tempDir, "designs/card.dac")
        val readmeFile = File(tempDir, "README.md")

        assertTrue(configFile.exists(), "vireo.config.json should exist")
        assertTrue(tokensFile.exists(), "tokens.dac should exist")
        assertTrue(buttonFile.exists(), "button.dac should exist")
        assertTrue(cardFile.exists(), "card.dac should exist")
        assertTrue(readmeFile.exists(), "README.md should exist")

        // Verify that the generated project card.dac passes vireo check!
        val (checkCode, checkOut, checkErr) = runCliTest("check", cardFile.absolutePath)
        assertEquals(0, checkCode, "check on initialized project should pass: $checkErr")
        assertTrue(checkOut.contains("passed analysis."))

        // Verify that card.dac renders to standard HTML
        val (renderCode, renderOut, renderErr) = runCliTest("render", cardFile.absolutePath, "--html")
        assertEquals(0, renderCode, "render on initialized project should pass: $renderErr")
        assertTrue(renderOut.startsWith("<!DOCTYPE html>"))
        assertTrue(renderOut.contains("Welcome to Vireo"))
        assertTrue(renderOut.contains("Open Docs"))
        assertTrue(renderOut.contains("href=\"https://github.com/DorMiwww/Vireo\""))

        // Cleanup
        tempDir.deleteRecursively()
    }
}
