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

        val analysisResult = Analyzer.analyze(file, fileLoader = fileLoader)
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

    @Test
    fun testRichMediaAndStackToFigma() {
        val dacSource = """
            block MediaBlock {
                component HeroBanner {
                    layout: stack
                    width: 400
                    height: 250

                    component BgImage {
                        image: "hero.png"
                        fit: cover
                        width: 400
                        height: 250
                        zIndex: 1
                    }

                    component OverlayCard {
                        x: 20
                        y: 30
                        width: 200
                        height: 80
                        color: #FFFFFF
                        zIndex: 2

                        component Title {
                            text: "Card on Image"
                            fontSize: 16
                        }
                    }

                    component PromoVideo {
                        video: "promo.mp4"
                        poster: "poster.jpg"
                        width: 300
                        height: 180
                        zIndex: 3
                    }
                }
            }
        """.trimIndent()

        val parseResult = Parser.parse(dacSource, "media.dac")
        assertTrue(parseResult is VireoResult.Ok)
        val file = parseResult.value

        val analysisResult = Analyzer.analyze(file, assetChecker = { true })
        assertTrue(analysisResult is VireoResult.Ok)
        val resolvedFile = analysisResult.value

        val renderResult = FigmaRenderer.render(resolvedFile)
        assertTrue(renderResult is VireoResult.Ok)
        val doc = renderResult.value

        assertEquals(2, doc.schemaVersion)
        val canvas = doc.nodes[0]
        val block = canvas.children?.get(0)
        assertNotNull(block)

        val heroBanner = block.children?.get(0)
        assertNotNull(heroBanner)
        assertEquals("HeroBanner", heroBanner.name)
        // Stack layout should not force VERTICAL or HORIZONTAL Auto Layout
        assertEquals(null, heroBanner.layoutMode)

        val heroChildren = heroBanner.children
        assertNotNull(heroChildren)
        assertEquals(3, heroChildren.size)

        // Check children ordered by zIndex
        val imgChild = heroChildren[0]
        assertEquals("BgImage", imgChild.name)
        assertEquals("RECTANGLE", imgChild.type)
        assertEquals("IMAGE", imgChild.mediaType)
        assertEquals("hero.png", imgChild.mediaUrl)
        assertEquals("FILL", imgChild.scaleMode)
        assertNotNull(imgChild.fills)
        assertEquals("IMAGE", imgChild.fills?.get(0)?.type)

        val cardChild = heroChildren[1]
        assertEquals("OverlayCard", cardChild.name)
        assertEquals(20f, cardChild.absoluteBoundingBox?.x)
        assertEquals(30f, cardChild.absoluteBoundingBox?.y)

        val videoChild = heroChildren[2]
        assertEquals("PromoVideo", videoChild.name)
        assertEquals("FRAME", videoChild.type)
        assertEquals("VIDEO", videoChild.mediaType)
        assertEquals("promo.mp4", videoChild.mediaUrl)

        val json = doc.toJson(pretty = true)
        assertTrue(json.contains("\"schemaVersion\": 2"))
        assertTrue(json.contains("\"mediaType\": \"IMAGE\""))
        assertTrue(json.contains("\"mediaType\": \"VIDEO\""))
        assertTrue(json.contains("\"scaleMode\": \"FILL\""))
    }
}

