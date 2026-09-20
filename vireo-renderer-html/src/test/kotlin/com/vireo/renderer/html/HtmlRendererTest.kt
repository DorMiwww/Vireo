package com.vireo.renderer.html

import com.vireo.analysis.Analyzer
import com.vireo.core.*
import com.vireo.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlRendererTest {

    @Test
    fun `test Example 3 - Auto Layout Card renders to HTML with Flexbox and styles`() {
        val source = """
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

        val parseResult = Parser.parse(source, "card.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parseResult)

        val analyzeResult = Analyzer.analyze(parseResult.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzeResult)

        val renderResult = HtmlRenderer.render(analyzeResult.value)
        assertIs<VireoResult.Ok<String>>(renderResult)

        val html = renderResult.value
        assertTrue(html.contains("data-path=\"card.dac\""))
        assertTrue(html.contains("data-name=\"Cards\""))
        assertTrue(html.contains("data-name=\"Card\""))
        assertTrue(html.contains("display: flex;"))
        assertTrue(html.contains("flex-direction: column;"))
        assertTrue(html.contains("gap: 12px;"))
        assertTrue(html.contains("padding: 16px;"))
        assertTrue(html.contains("border-radius: 12px;"))
        assertTrue(html.contains("box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);"))
        assertTrue(html.contains("Card Title"))
        assertTrue(html.contains("Card description goes here."))
        assertTrue(html.contains("font-size: 18px;"))
        assertTrue(html.contains("font-weight: bold;"))
        assertTrue(html.contains("color: #111827;"))
        assertTrue(html.contains("color: #6B7280;"))
    }

    @Test
    fun `test Example 4 - Cross-file reference Login Form renders to HTML`() {
        val buttonsSource = """
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

        val loginSource = """
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

        val parseResult = Parser.parse(loginSource, "login.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parseResult)

        val fileLoader: (String) -> String = { path ->
            if (path.endsWith("buttons.dac")) buttonsSource else throw IllegalArgumentException("Unknown file: $path")
        }

        val analyzeResult = Analyzer.analyze(parseResult.value, fileLoader)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzeResult)

        val renderResult = HtmlRenderer.render(analyzeResult.value)
        assertIs<VireoResult.Ok<String>>(renderResult)

        val html = renderResult.value
        assertTrue(html.contains("data-path=\"login.dac\""))
        assertTrue(html.contains("data-name=\"LoginForm\""))
        assertTrue(html.contains("data-name=\"Container\""))
        assertTrue(html.contains("width: 360px;"))
        assertTrue(html.contains("padding: 24px;"))
        assertTrue(html.contains("Sign In"))
        assertTrue(html.contains("font-size: 24px;"))
        assertTrue(html.contains("border: 1px solid #D1D5DB;"))
        assertTrue(html.contains("data-name=\"SubmitButton\""))
    }

    @Test
    fun `test HtmlRenderer with standardHtml true renders complete HTML5 document with title and DOCTYPE`() {
        val source = """
            block Test {
                component Box {
                    width: 100
                    height: 50
                }
            }
        """.trimIndent()
        val parseResult = Parser.parse(source, "box.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parseResult)
        val analyzeResult = Analyzer.analyze(parseResult.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzeResult)

        val options = HtmlRenderOptions(standardHtml = true, title = "Custom Box Title", theme = "dark")
        val renderResult = HtmlRenderer.render(analyzeResult.value, options)
        assertIs<VireoResult.Ok<String>>(renderResult)

        val html = renderResult.value
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("<title>Custom Box Title</title>"))
        assertTrue(html.contains("background-color: #111827;"))
        assertTrue(html.contains("class=\"vireo-file\""))
        assertTrue(html.endsWith("</html>"))
    }

    @Test
    fun `test HtmlRenderer with snippet mode renders only component container without DOCTYPE`() {
        val source = """
            block Test {
                component Box {
                    width: 100
                    height: 50
                }
            }
        """.trimIndent()
        val parseResult = Parser.parse(source, "box.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parseResult)
        val analyzeResult = Analyzer.analyze(parseResult.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzeResult)

        val options = HtmlRenderOptions(standardHtml = false)
        val renderResult = HtmlRenderer.render(analyzeResult.value, options)
        assertIs<VireoResult.Ok<String>>(renderResult)

        val html = renderResult.value
        assertTrue(!html.contains("<!DOCTYPE html>"))
        assertTrue(!html.contains("<head>"))
        assertTrue(!html.contains("<body>"))
        assertTrue(html.startsWith("<div class=\"vireo-file\""))
        assertTrue(html.endsWith("</div>"))
        assertTrue(html.contains("data-name=\"Box\""))
    }
}
