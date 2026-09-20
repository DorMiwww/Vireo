package com.vireo.renderer.figma

import com.vireo.analysis.Analyzer
import com.vireo.core.*
import com.vireo.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FigmaRendererTest {

    @Test
    fun testSimpleBoxToFigma() {
        val dacSource = """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: #3B82F6
                    radius: 8
                }
            }
        """.trimIndent()

        val parseResult = Parser.parse(dacSource, "test.dac")
        assertTrue(parseResult is VireoResult.Ok, "Parse failed")
        val file = parseResult.value

        val analysisResult = Analyzer.analyze(file)
        assertTrue(analysisResult is VireoResult.Ok, "Analysis failed")
        val resolvedFile = analysisResult.value

        val renderResult = FigmaRenderer.render(resolvedFile)
        assertTrue(renderResult is VireoResult.Ok, "Render failed")

        val doc = renderResult.value
        assertEquals("test.dac", doc.name)
        assertEquals(1, doc.nodes.size)

        val canvas = doc.nodes[0]
        assertEquals("CANVAS", canvas.type)
        val canvasChildren = canvas.children
        assertNotNull(canvasChildren)
        assertEquals(1, canvasChildren.size)

        val blockFrame = canvasChildren[0]
        assertEquals("Main", blockFrame.name)
        assertEquals("FRAME", blockFrame.type)

        val blockChildren = blockFrame.children
        assertNotNull(blockChildren)
        val boxNode = blockChildren[0]
        assertEquals("Box", boxNode.name)
        assertEquals(8f, boxNode.cornerRadius)
        val boxFills = boxNode.fills
        assertNotNull(boxFills)
        assertEquals(1, boxFills.size)
        assertEquals("SOLID", boxFills[0].type)

        val json = doc.toJson(pretty = true)
        assertTrue(json.contains("\"name\": \"Main\""))
        assertTrue(json.contains("\"cornerRadius\": 8"))
    }

    @Test
    fun testLoginFormToFigma() {
        val buttonsDac = """
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

        val loginDac = """
            import buttons from "./buttons.dac"

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

                    component SubmitButton {
                        ref: buttons.Primary.Default
                        label: "Sign In"
                        width: fill
                    }
                }
            }
        """.trimIndent()

        val fileLoader: (String) -> String = { path ->
            if (path == "./buttons.dac" || path == "buttons.dac") {
                buttonsDac
            } else {
                throw IllegalArgumentException("File not found: $path")
            }
        }

        val parseResult = Parser.parse(loginDac, "login.dac")
        assertTrue(parseResult is VireoResult.Ok)
        val file = parseResult.value

        val analysisResult = Analyzer.analyze(file, fileLoader)
        assertTrue(analysisResult is VireoResult.Ok)
        val resolvedFile = analysisResult.value

        val renderResult = FigmaRenderer.render(resolvedFile)
        assertTrue(renderResult is VireoResult.Ok)

        val doc = renderResult.value
        val json = doc.toJson(pretty = true)

        assertTrue(json.contains("LoginForm"))
        assertTrue(json.contains("Container"))
        assertTrue(json.contains("Sign In"))
        assertTrue(json.contains("VERTICAL"))
    }
}
