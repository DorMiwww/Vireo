package com.vireo.parser

import com.vireo.core.*
import com.vireo.lexer.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParserTest {

    @Test
    fun `test Example 1 - Hello Rectangle parses without errors`() {
        val source = """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: #3B82F6
                    radius: 8
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "hello_rectangle.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals("hello_rectangle.dac", file.path)
        assertEquals(0, file.imports.size)
        assertEquals(1, file.blocks.size)

        val block = file.blocks[0]
        assertEquals("Main", block.name)
        assertEquals(1, block.components.size)
        assertEquals(1, block.location.line)

        val box = block.components[0]
        assertEquals("Box", box.name)
        assertEquals(4, box.properties.size)
        assertEquals(2, box.constraints.size)
        assertEquals(0, box.children.size)

        // Verify properties
        val propMap = box.properties.associate { it.key to (it.value as PropertyValue.Literal).value }
        assertEquals(200, propMap["width"])
        assertEquals(100, propMap["height"])
        assertEquals("#3B82F6", propMap["color"])
        assertEquals(8, propMap["radius"])

        // Verify explicit constraints
        val explicitWidth = box.constraints.filterIsInstance<Constraint.Explicit>().find { it.axis == Axis.WIDTH }
        val explicitHeight = box.constraints.filterIsInstance<Constraint.Explicit>().find { it.axis == Axis.HEIGHT }
        assertEquals(200f, explicitWidth?.value)
        assertEquals(100f, explicitHeight?.value)

        // Verify locations
        assertEquals(2, box.location.line)
        assertEquals(3, box.properties[0].location.line) // width
    }

    @Test
    fun `test Example 2 - Button Component with nested child parses without errors`() {
        val source = """
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

        val result = Parser.parse(source, "buttons.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals("buttons.dac", file.path)
        assertEquals(1, file.blocks.size)

        val block = file.blocks[0]
        assertEquals("Primary", block.name)
        assertEquals(1, block.components.size)

        val defaultComp = block.components[0]
        assertEquals("Default", defaultComp.name)
        assertEquals(4, defaultComp.properties.size)
        assertEquals(1, defaultComp.children.size)

        // Verify Default component properties
        val defaultPropMap = defaultComp.properties.associate { it.key to (it.value as PropertyValue.Literal).value }
        assertEquals(120, defaultPropMap["width"])
        assertEquals(40, defaultPropMap["height"])
        assertEquals("#3B82F6", defaultPropMap["color"])
        assertEquals(8, defaultPropMap["radius"])

        val labelComp = defaultComp.children[0]
        assertEquals("Label", labelComp.name)
        assertEquals(4, labelComp.properties.size)
        assertEquals(0, labelComp.children.size)

        // Verify Label properties (including string, number, hex color, keyword)
        val labelPropMap = labelComp.properties.associate { it.key to (it.value as PropertyValue.Literal).value }
        assertEquals("Button", labelPropMap["text"])
        assertEquals(14, labelPropMap["fontSize"])
        assertEquals("bold", labelPropMap["fontWeight"])
        assertEquals("#FFFFFF", labelPropMap["color"])

        // Verify SourceLocations on nested component and properties
        assertEquals(8, labelComp.location.line)
        assertEquals(9, labelComp.properties.first { it.key == "text" }.location.line)
    }

    @Test
    fun `test error case malformed property missing colon returns VireoResult Err with a SourceLocation`() {
        val source = """
            block Main {
                component Box {
                    width 200
                    height: 100
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "malformed_prop.dac")
        assertIs<VireoResult.Err>(result)

        val errors = result.errors
        assertTrue(errors.isNotEmpty(), "Expected at least one error")

        val err = errors[0]
        assertEquals("malformed_prop.dac", err.location.file)
        assertEquals(3, err.location.line)
        assertEquals(9, err.location.column)
        assertTrue(err.message.contains("width"), "Error message should identify problematic token or element")
    }

    @Test
    fun `test pipeline run Lexer to Parser on raw dac source string end-to-end asserts resulting AST is correct`() {
        val rawSource = """
            import common from "./common.dac"

            block Main {
                component PrimaryButton {
                    width: 200
                    height: 48
                    color: #3B82F6

                    component Label {
                        text: "Submit"
                        fontSize: 16
                        color: #FFFFFF
                    }
                }
            }
        """.trimIndent()

        val lexResult = Lexer.tokenize(rawSource, "pipeline.dac")
        assertIs<VireoResult.Ok<List<Token>>>(lexResult)
        val tokens = lexResult.value

        val parseResult = Parser.parse(tokens, "pipeline.dac")
        assertIs<VireoResult.Ok<VireoFile>>(parseResult)
        val ast = parseResult.value

        assertEquals("pipeline.dac", ast.path)
        assertEquals(1, ast.imports.size)
        assertEquals("common", ast.imports[0].alias)
        assertEquals("./common.dac", ast.imports[0].filePath)

        assertEquals(1, ast.blocks.size)
        val block = ast.blocks[0]
        assertEquals("Main", block.name)

        assertEquals(1, block.components.size)
        val button = block.components[0]
        assertEquals("PrimaryButton", button.name)

        val buttonProps = button.properties.associate { it.key to (it.value as PropertyValue.Literal).value }
        assertEquals(200, buttonProps["width"])
        assertEquals(48, buttonProps["height"])
        assertEquals("#3B82F6", buttonProps["color"])

        assertEquals(1, button.children.size)
        val label = button.children[0]
        assertEquals("Label", label.name)
        val labelProps = label.properties.associate { it.key to (it.value as PropertyValue.Literal).value }
        assertEquals("Submit", labelProps["text"])
        assertEquals(16, labelProps["fontSize"])
        assertEquals("#FFFFFF", labelProps["color"])
    }

    @Test
    fun `test unresolved cross-file reference parsing`() {
        val source = """
            import buttons from "./buttons.dac"

            block LoginForm {
                component SubmitButton {
                    ref: buttons.Primary.Default
                    label: "Sign In"
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "login.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals(1, file.imports.size)
        assertEquals("buttons", file.imports[0].alias)
        assertEquals("./buttons.dac", file.imports[0].filePath)

        val submitButton = file.blocks[0].components[0]
        val refProp = submitButton.properties.first { it.key == "ref" }
        assertIs<PropertyValue.Ref>(refProp.value)

        val refVal = (refProp.value as PropertyValue.Ref).reference
        assertEquals("buttons", refVal.file)
        assertEquals("Primary", refVal.block)
        assertEquals("Default", refVal.component)
    }

    @Test
    fun `test multiple error collection with precise SourceLocations`() {
        val source = """
            block {
                component Box {
                    width: 
                    color: #3B82F6
                }
            }
            block ValidBlock {
                component {
                    height: 50
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "malformed.dac")
        assertIs<VireoResult.Err>(result)

        val errors = result.errors
        assertTrue(errors.size >= 2, "Expected at least 2 parse errors collected, found ${errors.size}")

        // Check first error (missing block name)
        assertTrue(errors.any { it.message.contains("Expected block name identifier") && it.location.line == 1 })

        // Check second error (missing component name in second block)
        assertTrue(errors.any { it.message.contains("Expected component name identifier") && it.location.line == 8 })

        // Ensure every error includes a SourceLocation
        errors.forEach { err ->
            assertEquals("malformed.dac", err.location.file)
            assertTrue(err.location.line > 0)
            assertTrue(err.location.column > 0)
        }
    }

    @Test
    fun `Task 2-B - parses import declarations producing Import AST nodes`() {
        val source = """
            import buttons from "./buttons.dac"
            import forms from "../forms.dac"

            block Main {
                component App {
                    width: 100
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "main.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals(2, file.imports.size)

        val imp1 = file.imports[0]
        assertEquals("buttons", imp1.alias)
        assertEquals("./buttons.dac", imp1.filePath)
        assertEquals(SourceLocation("main.dac", 1, 1), imp1.location)

        val imp2 = file.imports[1]
        assertEquals("forms", imp2.alias)
        assertEquals("../forms.dac", imp2.filePath)
        assertEquals(SourceLocation("main.dac", 2, 1), imp2.location)
    }

    @Test
    fun `Task 2-B - parses ref cross-file references producing Reference AST nodes`() {
        val source = """
            import buttons from "./buttons.dac"

            block Form {
                component SubmitButton {
                    ref: buttons.Primary.Default
                    label: "Submit"
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "form.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals(1, file.imports.size)
        assertEquals(1, file.blocks.size)

        val component = file.blocks[0].components[0]
        assertEquals("SubmitButton", component.name)

        val refProp = component.properties.first { it.key == "ref" }
        assertIs<PropertyValue.Ref>(refProp.value)

        val ref = (refProp.value as PropertyValue.Ref).reference
        assertEquals("buttons", ref.file)
        assertEquals("Primary", ref.block)
        assertEquals("Default", ref.component)
        assertEquals(SourceLocation("form.dac", 5, 14), ref.location)
    }

    @Test
    fun `Task 2-B - parses Example 4 from EXAMPLES md with imports and ref`() {
        val source = """
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

        val result = Parser.parse(source, "login.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals(1, file.imports.size)
        assertEquals("buttons", file.imports[0].alias)
        assertEquals("./buttons.dac", file.imports[0].filePath)

        val container = file.blocks[0].components[0]
        assertEquals("Container", container.name)
        assertEquals(4, container.children.size)

        val submitBtn = container.children.first { it.name == "SubmitButton" }
        val refProp = submitBtn.properties.first { it.key == "ref" }
        assertIs<PropertyValue.Ref>(refProp.value)
        val refNode = (refProp.value as PropertyValue.Ref).reference
        assertEquals("buttons", refNode.file)
        assertEquals("Primary", refNode.block)
        assertEquals("Default", refNode.component)
    }

    @Test
    fun `Task 2-B - returns VireoResult Err for malformed import declaration`() {
        val source = """
            import buttons "./buttons.dac"
        """.trimIndent()

        val result = Parser.parse(source, "invalid_import.dac")
        assertIs<VireoResult.Err>(result)

        val errors = result.errors
        assertTrue(errors.isNotEmpty())
        val err = errors[0]
        assertEquals("invalid_import.dac", err.location.file)
        assertTrue(err.message.contains("Expected 'from'"))
    }

    @Test
    fun `Task 2-D - Explicit sizing constraints width and height produce Constraint Explicit`() {
        val source = """
            block Main {
                component Element {
                    width: 120
                    height: 48
                    x: 10
                    y: 20
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "explicit.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val comp = result.value.blocks[0].components[0]
        assertEquals(4, comp.constraints.size)

        val explicitWidth = comp.constraints.filterIsInstance<Constraint.Explicit>().find { it.axis == Axis.WIDTH }
        val explicitHeight = comp.constraints.filterIsInstance<Constraint.Explicit>().find { it.axis == Axis.HEIGHT }
        val explicitX = comp.constraints.filterIsInstance<Constraint.Explicit>().find { it.axis == Axis.X }
        val explicitY = comp.constraints.filterIsInstance<Constraint.Explicit>().find { it.axis == Axis.Y }

        assertEquals(120f, explicitWidth?.value)
        assertEquals(48f, explicitHeight?.value)
        assertEquals(10f, explicitX?.value)
        assertEquals(20f, explicitY?.value)
    }

    @Test
    fun `Task 2-D - Auto layout properties produce Constraint AutoLayout`() {
        val source = """
            block Cards {
                component Card {
                    layout: vertical
                    mainAxis: fill
                    crossAxis: hug
                    gap: 16
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "autolayout.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val comp = result.value.blocks[0].components[0]
        val autoLayout = comp.constraints.filterIsInstance<Constraint.AutoLayout>().firstOrNull()

        assertTrue(autoLayout != null, "Expected Constraint.AutoLayout to be parsed")
        assertEquals(Direction.VERTICAL, autoLayout.direction)
        assertEquals(Sizing.FILL, autoLayout.mainAxis)
        assertEquals(Sizing.HUG, autoLayout.crossAxis)
        assertEquals(16f, autoLayout.gap)
    }

    @Test
    fun `Task 2-D - Relational properties produce Constraint Relational`() {
        val source = """
            block Layouts {
                component Panel {
                    width: 50%parent
                    x: parent.x + 16
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "relational.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val comp = result.value.blocks[0].components[0]
        val relationalWidth = comp.constraints.filterIsInstance<Constraint.Relational>().find { it.axis == Axis.WIDTH }
        val relationalX = comp.constraints.filterIsInstance<Constraint.Relational>().find { it.axis == Axis.X }

        assertTrue(relationalWidth != null, "Expected Constraint.Relational for width")
        assertEquals("50%parent", relationalWidth.expression)

        assertTrue(relationalX != null, "Expected Constraint.Relational for x")
        assertEquals("parent.x + 16", relationalX.expression)
    }

    @Test
    fun `Task 3-B - parses var declarations producing VarDeclaration AST nodes`() {
        val source = """
            var primaryColor = #3B82F6
            var spacing = 16

            block Main {
                component Box {
                    width: 100
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "vars.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals(2, file.vars.size)

        val var1 = file.vars[0]
        assertEquals("primaryColor", var1.name)
        assertIs<PropertyValue.Literal>(var1.value)
        assertEquals("#3B82F6", (var1.value as PropertyValue.Literal).value)
        assertEquals(SourceLocation("vars.dac", 1, 1), var1.location)

        val var2 = file.vars[1]
        assertEquals("spacing", var2.name)
        assertIs<PropertyValue.Literal>(var2.value)
        assertEquals(16, (var2.value as PropertyValue.Literal).value)
        assertEquals(SourceLocation("vars.dac", 2, 1), var2.location)
    }

    @Test
    fun `Task 3-C - parses fun declarations producing FunDeclaration AST nodes`() {
        val source = """
            fun spacing(multiplier: Int): Int { return 8 * multiplier }

            block Main {
                component Box {
                    width: 100
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "funs.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val file = result.value
        assertEquals(1, file.functions.size)

        val fn = file.functions[0]
        assertEquals("spacing", fn.name)
        assertEquals("Int", fn.returnType)
        assertEquals(1, fn.parameters.size)
        assertEquals("multiplier", fn.parameters[0].name)
        assertEquals("Int", fn.parameters[0].type)
        assertIs<PropertyValue.Expr>(fn.returnExpr)
        assertEquals("8 * multiplier", (fn.returnExpr as PropertyValue.Expr).source)
        assertEquals(SourceLocation("funs.dac", 1, 1), fn.location)
    }

    @Test
    fun `Task 3-D - parses conditional expressions producing ConditionalExpr AST nodes`() {
        val source = """
            block Main {
                component Button {
                    color: if ${"$"}variant == "primary" then #3B82F6 else #6B7280
                }
            }
        """.trimIndent()

        val result = Parser.parse(source, "cond.dac")
        assertIs<VireoResult.Ok<VireoFile>>(result)

        val button = result.value.blocks[0].components[0]
        val colorProp = button.properties.first { it.key == "color" }
        assertIs<PropertyValue.ConditionalExpr>(colorProp.value)

        val condExpr = colorProp.value as PropertyValue.ConditionalExpr
        assertIs<PropertyValue.Expr>(condExpr.condition)
        assertEquals("${"$"}variant == \"primary\"", (condExpr.condition as PropertyValue.Expr).source)

        assertIs<PropertyValue.Literal>(condExpr.thenBranch)
        assertEquals("#3B82F6", (condExpr.thenBranch as PropertyValue.Literal).value)

        assertIs<PropertyValue.Literal>(condExpr.elseBranch)
        assertEquals("#6B7280", (condExpr.elseBranch as PropertyValue.Literal).value)

        assertEquals(3, condExpr.location.line)
    }
}



