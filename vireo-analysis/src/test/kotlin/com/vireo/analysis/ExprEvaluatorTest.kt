package com.vireo.analysis

import com.vireo.core.*
import com.vireo.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExprEvaluatorTest {

    @Test
    fun `test variable substitution resolves correctly`() {
        val dac = """
            var primaryColor = #3B82F6
            var radiusBase = 8

            block Tokens {
                component Badge {
                    color: ${'$'}primaryColor
                    radius: ${'$'}radiusBase
                }
            }
        """.trimIndent()

        val file = (Parser.parse(dac) as VireoResult.Ok).value
        val evaluated = (ExprEvaluator.evaluate(file) as VireoResult.Ok).value

        val comp = evaluated.blocks[0].components[0]
        val colorProp = comp.properties.find { it.key == "color" }!!
        val radiusProp = comp.properties.find { it.key == "radius" }!!

        assertEquals("#3B82F6", (colorProp.value as PropertyValue.Literal).value)
        assertEquals(8, (radiusProp.value as PropertyValue.Literal).value)
    }

    @Test
    fun `test arithmetic and function calls evaluate correctly`() {
        val dac = """
            fun spacing(multiplier: Int): Int {
                return 8 * multiplier
            }

            block Card {
                component Box {
                    padding: spacing(2)
                    gap: 4 + 4
                }
            }
        """.trimIndent()

        val file = (Parser.parse(dac) as VireoResult.Ok).value
        val evaluated = (ExprEvaluator.evaluate(file) as VireoResult.Ok).value

        val comp = evaluated.blocks[0].components[0]
        val paddingProp = comp.properties.find { it.key == "padding" }!!
        val gapProp = comp.properties.find { it.key == "gap" }!!

        assertEquals(16, (paddingProp.value as PropertyValue.Literal).value)
        assertEquals(8, (gapProp.value as PropertyValue.Literal).value)
    }

    @Test
    fun `test conditional expressions evaluate correctly`() {
        val dacPrimary = """
            var variant = "primary"

            block Main {
                component Button {
                    color: if ${'$'}variant == "primary" then #3B82F6 else #6B7280
                }
            }
        """.trimIndent()

        val filePrimary = (Parser.parse(dacPrimary) as VireoResult.Ok).value
        val evalPrimary = (ExprEvaluator.evaluate(filePrimary) as VireoResult.Ok).value
        val btnPrimary = evalPrimary.blocks[0].components[0]
        val colorPrimary = btnPrimary.properties.find { it.key == "color" }!!
        assertEquals("#3B82F6", (colorPrimary.value as PropertyValue.Literal).value)

        val dacSecondary = """
            var variant = "secondary"

            block Main {
                component Button {
                    color: if ${'$'}variant == "primary" then #3B82F6 else #6B7280
                }
            }
        """.trimIndent()

        val fileSecondary = (Parser.parse(dacSecondary) as VireoResult.Ok).value
        val evalSecondary = (ExprEvaluator.evaluate(fileSecondary) as VireoResult.Ok).value
        val btnSecondary = evalSecondary.blocks[0].components[0]
        val colorSecondary = btnSecondary.properties.find { it.key == "color" }!!
        assertEquals("#6B7280", (colorSecondary.value as PropertyValue.Literal).value)
    }

    @Test
    fun `test string interpolation resolves variables`() {
        val dac = """
            var name = "World"

            block Main {
                component Label {
                    text: "Hello ${'$'}name"
                }
            }
        """.trimIndent()

        val file = (Parser.parse(dac) as VireoResult.Ok).value
        val evaluated = (ExprEvaluator.evaluate(file) as VireoResult.Ok).value

        val comp = evaluated.blocks[0].components[0]
        val textProp = comp.properties.find { it.key == "text" }!!
        assertEquals("Hello World", (textProp.value as PropertyValue.Literal).value)
    }

    @Test
    fun `test relational constraints remain as relational`() {
        val dac = """
            block Layouts {
                component SplitPanel {
                    component Left {
                        width: 50%parent
                    }
                }
            }
        """.trimIndent()

        val file = (Parser.parse(dac) as VireoResult.Ok).value
        val evaluated = (ExprEvaluator.evaluate(file) as VireoResult.Ok).value

        val comp = evaluated.blocks[0].components[0].children[0]
        val widthProp = comp.properties.find { it.key == "width" }!!
        assertEquals("50%parent", (widthProp.value as PropertyValue.Expr).source)
    }

    @Test
    fun `test evaluation error for undefined variable`() {
        val dac = """
            block Main {
                component Box {
                    color: ${'$'}nonExistentVar
                }
            }
        """.trimIndent()

        val file = (Parser.parse(dac) as VireoResult.Ok).value
        val evalResult = ExprEvaluator.evaluate(file)

        assertTrue(evalResult is VireoResult.Err)
        val errors = evalResult.errors
        assertTrue(errors.any { it.message.contains("Undefined variable") })
    }

    @Test
    fun `test evaluation error for division by zero`() {
        val dac = """
            block Main {
                component Box {
                    width: 10 / 0
                }
            }
        """.trimIndent()

        val file = (Parser.parse(dac) as VireoResult.Ok).value
        val evalResult = ExprEvaluator.evaluate(file)

        assertTrue(evalResult is VireoResult.Err)
        val errors = evalResult.errors
        assertTrue(errors.any { it.message.contains("Division by zero") })
    }
}
