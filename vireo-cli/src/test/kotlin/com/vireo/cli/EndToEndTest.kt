package com.vireo.cli

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndToEndTest {

    private fun runCli(vararg args: String): CliResult {
        val outStream = ByteArrayOutputStream()
        val errStream = ByteArrayOutputStream()
        val outPrint = PrintStream(outStream)
        val errPrint = PrintStream(errStream)

        val exitCode = executeCli(args.toList().toTypedArray(), outPrint, errPrint)
        outPrint.flush()
        errPrint.flush()

        return CliResult(exitCode, outStream.toString(), errStream.toString())
    }

    private data class CliResult(val exitCode: Int, val stdout: String, val stderr: String)

    @Test
    fun `test Example 1 - Hello Rectangle renders to correct JSON via CLI`() {
        val dacFile = File.createTempFile("example1_hello_rectangle", ".dac")
        dacFile.deleteOnExit()
        dacFile.writeText(
            """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: #3B82F6
                    radius: 8
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCli("render", dacFile.absolutePath, "--to", "json")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty(), "stderr should be empty, got: $stderr")

        val expectedJson = """
            {
              "blocks": [
                {
                  "name": "Main",
                  "components": [
                    {
                      "name": "Box",
                      "properties": {
                        "width": 200,
                        "height": 100,
                        "color": "#3B82F6",
                        "radius": 8
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals(expectedJson.trim(), stdout.trim())
    }

    @Test
    fun `test Example 2 - Button Component renders to correct JSON via CLI`() {
        val dacFile = File.createTempFile("example2_button", ".dac")
        dacFile.deleteOnExit()
        dacFile.writeText(
            """
            block Primary {
                component Default {
                    width: 120
                    height: 40
                    color: #3B82F6
                    radius: 8

                    component Label {
                        text: "Button"
                        fontSize: 14
                        fontWeight: bold
                        color: #FFFFFF
                    }
                }
            }
            """.trimIndent()
        )

        val (exitCode, stdout, stderr) = runCli("render", dacFile.absolutePath, "--to", "json")

        assertEquals(0, exitCode)
        assertTrue(stderr.isEmpty(), "stderr should be empty, got: $stderr")

        val expectedJson = """
            {
              "blocks": [
                {
                  "name": "Primary",
                  "components": [
                    {
                      "name": "Default",
                      "properties": {
                        "width": 120,
                        "height": 40,
                        "color": "#3B82F6",
                        "radius": 8
                      },
                      "children": [
                        {
                          "name": "Label",
                          "properties": {
                            "text": "Button",
                            "fontSize": 14,
                            "fontWeight": "bold",
                            "color": "#FFFFFF"
                          }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals(expectedJson.trim(), stdout.trim())
    }

    @Test
    fun `test Example 1 and Example 2 render to JSON file via CLI output option`() {
        val example1File = File.createTempFile("example1", ".dac")
        example1File.deleteOnExit()
        example1File.writeText(
            """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: #3B82F6
                    radius: 8
                }
            }
            """.trimIndent()
        )

        val outputFile = File.createTempFile("example1_output", ".json")
        outputFile.deleteOnExit()

        val (exitCode, stdout, stderr) = runCli("render", example1File.absolutePath, "--to", "json", "-o", outputFile.absolutePath)

        assertEquals(0, exitCode)
        assertTrue(stdout.isEmpty())
        assertTrue(stderr.isEmpty())

        val writtenContent = outputFile.readText()
        assertTrue(writtenContent.contains("\"name\": \"Main\""))
        assertTrue(writtenContent.contains("\"name\": \"Box\""))
        assertTrue(writtenContent.contains("\"color\": \"#3B82F6\""))
    }

    @Test
    fun `test Example 3 - Auto Layout Card renders to valid JSON and HTML via CLI`() {
        val dacFile = File.createTempFile("example3_card", ".dac")
        dacFile.deleteOnExit()
        dacFile.writeText(
            """
            block Cards {
                component Card {
                    layout: vertical
                    mainAxis: hug
                    crossAxis: fill
                    gap: 12
                    padding: 16
                    color: #FFFFFF
                    radius: 12
                    shadow: true

                    component Title {
                        text: "Card Title"
                        fontSize: 18
                        fontWeight: bold
                        color: #111827
                    }

                    component Description {
                        text: "Card description goes here."
                        fontSize: 14
                        color: #6B7280
                    }
                }
            }
            """.trimIndent()
        )

        // 1. JSON render via CLI
        val (jsonExitCode, jsonStdout, jsonStderr) = runCli("render", dacFile.absolutePath, "--to", "json")
        assertEquals(0, jsonExitCode, "JSON render CLI failed: $jsonStderr")
        assertTrue(jsonStderr.isEmpty(), "stderr should be empty for JSON render, got: $jsonStderr")
        assertTrue(jsonStdout.contains("\"name\": \"Cards\""))
        assertTrue(jsonStdout.contains("\"name\": \"Card\""))
        assertTrue(jsonStdout.contains("\"layout\": \"vertical\""))
        assertTrue(jsonStdout.contains("\"mainAxis\": \"hug\""))
        assertTrue(jsonStdout.contains("\"crossAxis\": \"fill\""))
        assertTrue(jsonStdout.contains("\"gap\": 12"))
        assertTrue(jsonStdout.contains("\"padding\": 16"))
        assertTrue(jsonStdout.contains("\"color\": \"#FFFFFF\""))
        assertTrue(jsonStdout.contains("\"radius\": 12"))
        assertTrue(jsonStdout.contains("\"shadow\": true"))
        assertTrue(jsonStdout.contains("\"name\": \"Title\""))
        assertTrue(jsonStdout.contains("\"text\": \"Card Title\""))
        assertTrue(jsonStdout.contains("\"name\": \"Description\""))
        assertTrue(jsonStdout.contains("\"text\": \"Card description goes here.\""))

        // 2. HTML render via CLI
        val (htmlExitCode, htmlStdout, htmlStderr) = runCli("render", dacFile.absolutePath, "--to", "html")
        assertEquals(0, htmlExitCode, "HTML render CLI failed: $htmlStderr")
        assertTrue(htmlStderr.isEmpty(), "stderr should be empty for HTML render, got: $htmlStderr")
        assertTrue(htmlStdout.contains("class=\"vireo-file\""))
        assertTrue(htmlStdout.contains("data-name=\"Cards\""))
        assertTrue(htmlStdout.contains("data-name=\"Card\""))
        assertTrue(htmlStdout.contains("display: flex;"))
        assertTrue(htmlStdout.contains("flex-direction: column;"))
        assertTrue(htmlStdout.contains("gap: 12px;"))
        assertTrue(htmlStdout.contains("padding: 16px;"))
        assertTrue(htmlStdout.contains("border-radius: 12px;"))
        assertTrue(htmlStdout.contains("Card Title"))
        assertTrue(htmlStdout.contains("Card description goes here."))

        // 3. vireo check via CLI
        val (checkExitCode, checkStdout, checkStderr) = runCli("check", dacFile.absolutePath)
        assertEquals(0, checkExitCode, "check CLI failed: $checkStderr")
        assertTrue(checkStderr.isEmpty(), "stderr should be empty for check, got: $checkStderr")
        assertTrue(checkStdout.contains("OK: ${dacFile.absolutePath} passed analysis."))
    }

    @Test
    fun `test Example 4 - Cross-file Reference login form renders to valid JSON and HTML via CLI`() {
        val tempDir = System.getProperty("java.io.tmpdir")
        val buttonsFile = File(tempDir, "buttons_${System.currentTimeMillis()}.dac")
        buttonsFile.deleteOnExit()
        buttonsFile.writeText(
            """
            block Primary {
                component Default {
                    width: 200
                    height: 44
                    color: #3B82F6
                    radius: 8

                    component Label {
                        text: "Button"
                        fontSize: 14
                        fontWeight: bold
                        color: #FFFFFF
                    }
                }
            }
            """.trimIndent()
        )

        val loginFile = File(tempDir, "login_${System.currentTimeMillis()}.dac")
        loginFile.deleteOnExit()
        loginFile.writeText(
            """
            import buttons from "./${buttonsFile.name}"

            block LoginForm {
                component Container {
                    layout: vertical
                    gap: 16
                    padding: 24
                    width: 360

                    component Title {
                        text: "Sign In"
                        fontSize: 24
                        fontWeight: bold
                        color: #111827
                    }

                    component EmailField {
                        width: fill
                        height: 44
                        radius: 8
                        border: 1 #D1D5DB
                        placeholder: "Email"
                    }

                    component PasswordField {
                        width: fill
                        height: 44
                        radius: 8
                        border: 1 #D1D5DB
                        placeholder: "Password"
                    }

                    component SubmitButton {
                        ref: buttons.Primary.Default
                        label: "Sign In"
                        width: fill
                    }
                }
            }
            """.trimIndent()
        )

        // 1. JSON render via CLI
        val (jsonExitCode, jsonStdout, jsonStderr) = runCli("render", loginFile.absolutePath, "--to", "json")
        assertEquals(0, jsonExitCode, "JSON render CLI failed: $jsonStderr")
        assertTrue(jsonStderr.isEmpty(), "stderr should be empty for JSON render, got: $jsonStderr")
        assertTrue(jsonStdout.contains("\"alias\": \"buttons\""))
        assertTrue(jsonStdout.contains("\"name\": \"LoginForm\""))
        assertTrue(jsonStdout.contains("\"name\": \"Container\""))
        assertTrue(jsonStdout.contains("\"name\": \"SubmitButton\""))
        assertTrue(jsonStdout.contains("\"ref: buttons.Primary.Default\""))
        assertTrue(jsonStdout.contains("\"label\": \"Sign In\""))

        // 2. HTML render via CLI
        val (htmlExitCode, htmlStdout, htmlStderr) = runCli("render", loginFile.absolutePath, "--to", "html")
        assertEquals(0, htmlExitCode, "HTML render CLI failed: $htmlStderr")
        assertTrue(htmlStderr.isEmpty(), "stderr should be empty for HTML render, got: $htmlStderr")
        assertTrue(htmlStdout.contains("class=\"vireo-file\""))
        assertTrue(htmlStdout.contains("data-name=\"LoginForm\""))
        assertTrue(htmlStdout.contains("data-name=\"Container\""))
        assertTrue(htmlStdout.contains("width: 360px;"))
        assertTrue(htmlStdout.contains("padding: 24px;"))
        assertTrue(htmlStdout.contains("Sign In"))
        assertTrue(htmlStdout.contains("font-size: 24px;"))
        assertTrue(htmlStdout.contains("data-name=\"SubmitButton\""))

        // 3. vireo check via CLI
        val (checkExitCode, checkStdout, checkStderr) = runCli("check", loginFile.absolutePath)
        assertEquals(0, checkExitCode, "check CLI failed: $checkStderr")
        assertTrue(checkStderr.isEmpty(), "stderr should be empty for check, got: $checkStderr")
        assertTrue(checkStdout.contains("OK: ${loginFile.absolutePath} passed analysis."))
    }
}
