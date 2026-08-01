package com.vireo.renderer.json

import com.vireo.core.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JsonRendererTest {

    @Test
    fun `test Example 1 - Hello Rectangle renders to expected JSON format`() {
        val loc = SourceLocation("hello_rectangle.dac", 1, 1)
        val file = VireoFile(
            path = "hello_rectangle.dac",
            imports = emptyList(),
            blocks = listOf(
                Block(
                    name = "Main",
                    components = listOf(
                        ComponentNode(
                            name = "Box",
                            properties = listOf(
                                Property("width", PropertyValue.Literal(200, loc), loc),
                                Property("height", PropertyValue.Literal(100, loc), loc),
                                Property("color", PropertyValue.Literal("#3B82F6", loc), loc),
                                Property("radius", PropertyValue.Literal(8, loc), loc)
                            ),
                            constraints = emptyList(),
                            children = emptyList(),
                            location = loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val result = JsonRenderer.render(file)
        assertIs<VireoResult.Ok<String>>(result)

        val json = result.value
        assertTrue(json.contains("\"name\": \"Main\""))
        assertTrue(json.contains("\"name\": \"Box\""))
        assertTrue(json.contains("\"width\": 200"))
        assertTrue(json.contains("\"height\": 100"))
        assertTrue(json.contains("\"color\": \"#3B82F6\""))
        assertTrue(json.contains("\"radius\": 8"))
    }

    @Test
    fun `test Example 2 - Button Component with nested children renders correctly`() {
        val loc = SourceLocation("buttons.dac", 1, 1)
        val file = VireoFile(
            path = "buttons.dac",
            imports = emptyList(),
            blocks = listOf(
                Block(
                    name = "Primary",
                    components = listOf(
                        ComponentNode(
                            name = "Default",
                            properties = listOf(
                                Property("width", PropertyValue.Literal(120, loc), loc),
                                Property("height", PropertyValue.Literal(40, loc), loc),
                                Property("color", PropertyValue.Literal("#3B82F6", loc), loc),
                                Property("radius", PropertyValue.Literal(8, loc), loc)
                            ),
                            constraints = emptyList(),
                            children = listOf(
                                ComponentNode(
                                    name = "Label",
                                    properties = listOf(
                                        Property("text", PropertyValue.Literal("Button", loc), loc),
                                        Property("fontSize", PropertyValue.Literal(14, loc), loc),
                                        Property("fontWeight", PropertyValue.Literal("bold", loc), loc),
                                        Property("color", PropertyValue.Literal("#FFFFFF", loc), loc)
                                    ),
                                    constraints = emptyList(),
                                    children = emptyList(),
                                    location = loc
                                )
                            ),
                            location = loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val result = JsonRenderer.render(file)
        assertIs<VireoResult.Ok<String>>(result)

        val json = result.value
        assertTrue(json.contains("\"name\": \"Primary\""))
        assertTrue(json.contains("\"name\": \"Default\""))
        assertTrue(json.contains("\"children\": ["))
        assertTrue(json.contains("\"name\": \"Label\""))
        assertTrue(json.contains("\"text\": \"Button\""))
        assertTrue(json.contains("\"fontWeight\": \"bold\""))
    }

    @Test
    fun `test render via ResolvedFile interface`() {
        val loc = SourceLocation("test.dac", 1, 1)
        val file = VireoFile("test.dac", emptyList(), blocks = listOf(Block("TestBlock", emptyList(), loc)), location = loc)
        val resolved = ResolvedFile(file)

        val result: VireoResult<String> = JsonRenderer.render(resolved)
        assertIs<VireoResult.Ok<String>>(result)
        assertTrue(result.value.contains("\"name\": \"TestBlock\""))
    }

    @Test
    fun `test render imports and references`() {
        val loc = SourceLocation("login.dac", 1, 1)
        val file = VireoFile(
            path = "login.dac",
            imports = listOf(Import("buttons", "./buttons.dac", loc)),
            blocks = listOf(
                Block(
                    name = "LoginForm",
                    components = listOf(
                        ComponentNode(
                            name = "SubmitButton",
                            properties = listOf(
                                Property(
                                    "ref",
                                    PropertyValue.Ref(Reference("buttons", "Primary", "Default", loc), loc),
                                    loc
                                )
                            ),
                            constraints = emptyList(),
                            children = emptyList(),
                            location = loc
                        )
                    ),
                    location = loc
                )
            ),
            location = loc
        )

        val result = JsonRenderer.render(file)
        assertIs<VireoResult.Ok<String>>(result)
        val json = result.value
        assertTrue(json.contains("\"alias\": \"buttons\""))
        assertTrue(json.contains("\"filePath\": \"./buttons.dac\""))
        assertTrue(json.contains("\"ref: buttons.Primary.Default\""))
    }

    @Test
    fun `test render JSON with vars functions and ConditionalExpr`() {
        val loc = SourceLocation("test.dac", 1, 1)
        val varDecl = VarDeclaration("primaryColor", PropertyValue.Literal("#3B82F6", loc), loc)
        val funDecl = FunDeclaration("spacing", listOf(Parameter("multiplier", "Int", loc)), "Int", PropertyValue.Expr("8 * multiplier", loc), loc)
        val condExpr = PropertyValue.ConditionalExpr(
            PropertyValue.Expr("\$variant == \"primary\"", loc),
            PropertyValue.Literal("#3B82F6", loc),
            PropertyValue.Literal("#6B7280", loc),
            loc
        )
        val prop = Property("color", condExpr, loc)
        val comp = ComponentNode("Button", listOf(prop), emptyList(), emptyList(), loc)
        val block = Block("Main", listOf(comp), loc)
        val file = VireoFile("test.dac", emptyList(), listOf(varDecl), listOf(funDecl), listOf(block), loc)

        val result = JsonRenderer.render(file)
        assertIs<VireoResult.Ok<String>>(result)
        val json = result.value

        assertTrue(json.contains("\"vars\": ["))
        assertTrue(json.contains("\"name\": \"primaryColor\""))
        assertTrue(json.contains("\"functions\": ["))
        assertTrue(json.contains("\"name\": \"spacing\""))
        assertTrue(json.contains("\"type\": \"conditional\""))
    }
}
