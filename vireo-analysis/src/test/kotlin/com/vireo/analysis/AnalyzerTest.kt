package com.vireo.analysis

import com.vireo.core.VireoResult
import com.vireo.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyzerTest {

    @Test
    fun testValidResolution() {
        val loginSource = """
            import buttons from "./buttons.dac"

            block LoginForm {
                component SubmitButton {
                    ref: buttons.Primary.Default
                }
            }
        """.trimIndent()

        val buttonsSource = """
            block Primary {
                component Default {
                    width: 120
                    height: 40
                }
            }
        """.trimIndent()

        val files = mapOf(
            "login.dac" to loginSource,
            "buttons.dac" to buttonsSource
        )

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value) { path ->
            files[path] ?: throw IllegalArgumentException("File not found: $path")
        }

        assertTrue(analysisResult is VireoResult.Ok, "Expected Ok but got $analysisResult")
    }

    @Test
    fun testMissingComponentReference() {
        val loginSource = """
            import buttons from "./buttons.dac"

            block LoginForm {
                component SubmitButton {
                    ref: buttons.Primary.NonExistentComp
                }
            }
        """.trimIndent()

        val buttonsSource = """
            block Primary {
                component Default {
                    width: 120
                }
            }
        """.trimIndent()

        val files = mapOf(
            "login.dac" to loginSource,
            "buttons.dac" to buttonsSource
        )

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value) { path ->
            files[path] ?: throw IllegalArgumentException("File not found: $path")
        }

        val errors = when (analysisResult) {
            is VireoResult.Err -> analysisResult.errors
            else -> throw AssertionError("Expected Err but got $analysisResult")
        }
        assertEquals(1, errors.size)
        assertTrue(errors[0].message.contains("Component 'NonExistentComp' not found"))
    }

    @Test
    fun testMissingBlockReference() {
        val loginSource = """
            import buttons from "./buttons.dac"

            block LoginForm {
                component SubmitButton {
                    ref: buttons.NonExistentBlock.Default
                }
            }
        """.trimIndent()

        val buttonsSource = """
            block Primary {
                component Default {
                    width: 120
                }
            }
        """.trimIndent()

        val files = mapOf(
            "login.dac" to loginSource,
            "buttons.dac" to buttonsSource
        )

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value) { path ->
            files[path] ?: throw IllegalArgumentException("File not found: $path")
        }

        val errors = when (analysisResult) {
            is VireoResult.Err -> analysisResult.errors
            else -> throw AssertionError("Expected Err but got $analysisResult")
        }
        assertEquals(1, errors.size)
        assertTrue(errors[0].message.contains("Block 'NonExistentBlock' not found"))
    }

    @Test
    fun testUnknownImportAlias() {
        val loginSource = """
            block LoginForm {
                component SubmitButton {
                    ref: unknownAlias.Primary.Default
                }
            }
        """.trimIndent()

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value)

        val errors = when (analysisResult) {
            is VireoResult.Err -> analysisResult.errors
            else -> throw AssertionError("Expected Err but got $analysisResult")
        }
        assertEquals(1, errors.size)
        assertTrue(errors[0].message.contains("Unknown import alias 'unknownAlias'"))
    }

    @Test
    fun testMissingImportedFile() {
        val loginSource = """
            import buttons from "./missing_buttons.dac"

            block LoginForm {
                component SubmitButton {
                    ref: buttons.Primary.Default
                }
            }
        """.trimIndent()

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value) { path ->
            throw java.io.FileNotFoundException("File $path not found")
        }

        val errors = when (analysisResult) {
            is VireoResult.Err -> analysisResult.errors
            else -> throw AssertionError("Expected Err but got $analysisResult")
        }
        // Should collect missing file error AND unknown/unloaded alias reference error without stopping
        assertTrue(errors.any { it.message.contains("not found") })
    }

    @Test
    fun testCircularImport() {
        val fileASource = """
            import fileB from "./b.dac"

            block BlockA {
                component CompA {
                    width: 100
                }
            }
        """.trimIndent()

        val fileBSource = """
            import fileA from "./a.dac"

            block BlockB {
                component CompB {
                    width: 200
                }
            }
        """.trimIndent()

        val files = mapOf(
            "a.dac" to fileASource,
            "b.dac" to fileBSource
        )

        val parseResult = Parser.parse(fileASource, "a.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value) { path ->
            files[path] ?: throw java.io.FileNotFoundException("File $path not found")
        }

        val errors = when (analysisResult) {
            is VireoResult.Err -> analysisResult.errors
            else -> throw AssertionError("Expected Err but got $analysisResult")
        }
        assertTrue(errors.any { it.message.contains("Circular import detected") })
    }

    @Test
    fun testCollectsAllErrors() {
        val loginSource = """
            import buttons from "./buttons.dac"

            block LoginForm {
                component SubmitButton {
                    ref: buttons.Primary.MissingComp1
                }
                component ResetButton {
                    ref: buttons.Primary.MissingComp2
                }
            }
        """.trimIndent()

        val buttonsSource = """
            block Primary {
                component Default {
                    width: 120
                }
            }
        """.trimIndent()

        val files = mapOf(
            "login.dac" to loginSource,
            "buttons.dac" to buttonsSource
        )

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)

        val analysisResult = Analyzer.analyze(parseResult.value) { path ->
            files[path] ?: throw java.io.FileNotFoundException("File $path not found")
        }

        val errors = when (analysisResult) {
            is VireoResult.Err -> analysisResult.errors
            else -> throw AssertionError("Expected Err but got $analysisResult")
        }
        assertEquals(2, errors.size, "Should collect all 2 errors instead of stopping at 1")
    }
}
