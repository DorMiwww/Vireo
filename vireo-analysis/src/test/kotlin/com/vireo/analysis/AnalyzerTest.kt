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

        val analysisResult = Analyzer.analyze(parseResult.value, fileLoader = { path ->
            throw java.io.FileNotFoundException("File $path not found")
        })

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

    @Test
    fun testValidRichMediaAssets() {
        val source = """
            block MediaBlock {
                component ValidCard {
                    layout: stack
                    width: 400
                    height: 300
                    zIndex: 1

                    component Bg {
                        backgroundImage: "./assets/photo.jpg"
                        fit: cover
                    }
                    component Avatar {
                        src: "./assets/avatar.png"
                        fit: contain
                    }
                    component RemoteVideo {
                        src: "https://example.com/stream.mp4"
                        poster: "https://example.com/poster.jpg"
                    }
                    component Embed {
                        iframe: "https://www.youtube.com/embed/xyz"
                    }
                }
            }
        """.trimIndent()

        val parsed = Parser.parse(source, "media.dac")
        assertTrue(parsed is VireoResult.Ok)

        // Pass mock assetChecker where relative asset paths exist
        val result = Analyzer.analyze(parsed.value, assetChecker = { true })
        assertTrue(result is VireoResult.Ok, "Expected Ok but got: $result")
    }

    @Test
    fun testMissingLocalAssetError() {
        val source = """
            block MediaBlock {
                component MissingPhoto {
                    src: "./assets/nonexistent.png"
                }
            }
        """.trimIndent()

        val parsed = Parser.parse(source, "designs/test.dac")
        assertTrue(parsed is VireoResult.Ok)

        val result = Analyzer.analyze(parsed.value, assetChecker = { false })
        assertTrue(result is VireoResult.Err)
        val err = (result as VireoResult.Err).errors.first()
        assertTrue(err.message.contains("Asset file './assets/nonexistent.png' not found"))
    }

    @Test
    fun testUnsupportedAssetFormatError() {
        val source = """
            block MediaBlock {
                component BadFile {
                    src: "./assets/program.exe"
                }
            }
        """.trimIndent()

        val parsed = Parser.parse(source, "test.dac")
        assertTrue(parsed is VireoResult.Ok)

        val result = Analyzer.analyze(parsed.value, assetChecker = { true })
        assertTrue(result is VireoResult.Err)
        val err = (result as VireoResult.Err).errors.first()
        assertTrue(err.message.contains("Unsupported asset format '.exe'"))
    }

    @Test
    fun testInvalidFitPropertyError() {
        val source = """
            block MediaBlock {
                component Box {
                    fit: stretched
                }
            }
        """.trimIndent()

        val parsed = Parser.parse(source, "test.dac")
        assertTrue(parsed is VireoResult.Ok)

        val result = Analyzer.analyze(parsed.value)
        assertTrue(result is VireoResult.Err)
        val err = (result as VireoResult.Err).errors.first()
        assertTrue(err.message.contains("Property 'fit' expects 'cover', 'contain', 'fill', or 'none'"))
    }
}
