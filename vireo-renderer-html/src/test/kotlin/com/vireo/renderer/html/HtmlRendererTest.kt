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

        val analyzeResult = Analyzer.analyze(parseResult.value, fileLoader = fileLoader)
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

    @Test
    fun `test HtmlRenderer renders clickable component with href as anchor tag`() {
        val source = """
            block Links {
                component GitHubButton {
                    layout: horizontal
                    width: 160
                    height: 40
                    color: #24292F
                    radius: 6
                    href: "https://github.com/DorMiwww/Vireo"

                    component Label {
                        text: "View on GitHub"
                        color: #FFFFFF
                    }
                }
            }
        """.trimIndent()
        val parseResult = Parser.parse(source, "link.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parseResult)
        val analyzeResult = Analyzer.analyze(parseResult.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzeResult)

        val renderResult = HtmlRenderer.render(analyzeResult.value)
        assertIs<VireoResult.Ok<String>>(renderResult)

        val html = renderResult.value
        assertTrue(html.contains("<a class=\"vireo-component\" data-name=\"GitHubButton\" href=\"https://github.com/DorMiwww/Vireo\" target=\"_blank\" rel=\"noopener noreferrer\""))
        assertTrue(html.contains("cursor: pointer;"))
        assertTrue(html.contains("text-decoration: none;"))
        assertTrue(html.contains("View on GitHub"))
        assertTrue(html.contains("</a>"))
    }

    @Test
    fun `test HtmlRenderer renders standalone image with fit alt and radius`() {
        val source = """
            block Media {
                component Avatar {
                    src: "https://example.com/avatar.png"
                    width: 64
                    height: 64
                    radius: 32
                    fit: cover
                    alt: "User Avatar"
                }
            }
        """.trimIndent()
        val parsed = Parser.parse(source, "avatar.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parsed)
        val analyzed = Analyzer.analyze(parsed.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzed)

        val rendered = HtmlRenderer.render(analyzed.value)
        assertIs<VireoResult.Ok<String>>(rendered)
        val html = rendered.value
        assertTrue(html.contains("<img class=\"vireo-component\" data-name=\"Avatar\" src=\"https://example.com/avatar.png\" alt=\"User Avatar\""))
        assertTrue(html.contains("object-fit: cover;"))
        assertTrue(html.contains("border-radius: 32px;"))
    }

    @Test
    fun `test HtmlRenderer renders container with background image and children on top`() {
        val source = """
            block BannerBlock {
                component HeroBanner {
                    backgroundImage: "https://example.com/hero.jpg"
                    fit: cover
                    layout: vertical
                    width: 600
                    height: 300

                    component Title {
                        text: "Welcome Home"
                        color: #FFFFFF
                    }
                }
            }
        """.trimIndent()
        val parsed = Parser.parse(source, "banner.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parsed)
        val analyzed = Analyzer.analyze(parsed.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzed)

        val rendered = HtmlRenderer.render(analyzed.value)
        assertIs<VireoResult.Ok<String>>(rendered)
        val html = rendered.value
        assertTrue(html.contains("background-image: url('https://example.com/hero.jpg');"))
        assertTrue(html.contains("background-size: cover;"))
        assertTrue(html.contains("Welcome Home"))
    }

    @Test
    fun `test HtmlRenderer renders video and audio and iframe with youtube conversion`() {
        val source = """
            block Players {
                component VideoBox {
                    src: "https://example.com/trailer.mp4"
                    poster: "https://example.com/poster.jpg"
                    controls: true
                    autoplay: true
                    loop: true
                    muted: true
                    width: 640
                    height: 360
                }

                component AudioBox {
                    src: "https://example.com/song.mp3"
                    controls: true
                    width: 300
                    height: 48
                }

                component YouTubeBox {
                    iframe: "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                    allowFullscreen: true
                    width: 560
                    height: 315
                }
            }
        """.trimIndent()
        val parsed = Parser.parse(source, "players.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parsed)
        val analyzed = Analyzer.analyze(parsed.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzed)

        val rendered = HtmlRenderer.render(analyzed.value)
        assertIs<VireoResult.Ok<String>>(rendered)
        val html = rendered.value

        // Video assertions
        assertTrue(html.contains("<video class=\"vireo-component\" data-name=\"VideoBox\" src=\"https://example.com/trailer.mp4\" poster=\"https://example.com/poster.jpg\" controls autoplay loop muted"))

        // Audio assertions
        assertTrue(html.contains("<audio class=\"vireo-component\" data-name=\"AudioBox\" src=\"https://example.com/song.mp3\" controls"))

        // iFrame assertions (youtube watch url converted to /embed/)
        assertTrue(html.contains("<iframe class=\"vireo-component\" data-name=\"YouTubeBox\" src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\" frameborder=\"0\""))
        assertTrue(html.contains("allowfullscreen"))
    }

    @Test
    fun `test HtmlRenderer renders stack layout with zIndex and absolute overlay`() {
        val source = """
            block StackBlock {
                component LayeredCard {
                    layout: stack
                    width: 400
                    height: 300

                    component BottomPhoto {
                        src: "https://example.com/photo.jpg"
                        width: fill
                        height: fill
                        zIndex: 0
                    }

                    component OverlaidText {
                        x: 20
                        y: 40
                        zIndex: 2
                        text: "Floating on top"
                        color: #FFFFFF
                    }
                }
            }
        """.trimIndent()
        val parsed = Parser.parse(source, "stack.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parsed)
        val analyzed = Analyzer.analyze(parsed.value)
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzed)

        val rendered = HtmlRenderer.render(analyzed.value)
        assertIs<VireoResult.Ok<String>>(rendered)
        val html = rendered.value

        assertTrue(html.contains("data-name=\"LayeredCard\""))
        assertTrue(html.contains("position: relative;"))
        assertTrue(html.contains("z-index: 0;"))
        assertTrue(html.contains("z-index: 2;"))
        assertTrue(html.contains("left: 20px;"))
        assertTrue(html.contains("top: 40px;"))
        assertTrue(html.contains("Floating on top"))
    }

    @Test
    fun `test HtmlRenderer with custom assetResolver embeds data URIs`() {
        val source = """
            block EmbedBlock {
                component Photo {
                    src: "./local/photo.png"
                    width: 100
                    height: 100
                }
            }
        """.trimIndent()
        val parsed = Parser.parse(source, "embed.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parsed)
        val analyzed = Analyzer.analyze(parsed.value, assetChecker = { true })
        assertIs<VireoResult.Ok<ResolvedFile>>(analyzed)

        val options = HtmlRenderOptions(
            assetResolver = { "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==" }
        )
        val rendered = HtmlRenderer.render(analyzed.value, options)
        assertIs<VireoResult.Ok<String>>(rendered)
        val html = rendered.value
        assertTrue(html.contains("src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==\""))
    }
}
