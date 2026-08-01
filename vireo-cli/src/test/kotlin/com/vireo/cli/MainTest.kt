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
}
