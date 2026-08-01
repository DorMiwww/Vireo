package com.vireo.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AstTest {

    @Test
    fun `test AST node creation and SourceLocation accessibility`() {
        val loc = SourceLocation("test.dac", 1, 1)
        val ref = Reference("components", "buttons", "primary", loc)
        val propValueRef = PropertyValue.Ref(ref)
        val prop = Property("icon", propValueRef, loc)

        val explicitConstraint = Constraint.Explicit(Axis.WIDTH, 120f, loc)
        val relationalConstraint = Constraint.Relational(Axis.HEIGHT, "parent.height * 0.5", loc)
        val autoLayoutConstraint = Constraint.AutoLayout(Direction.HORIZONTAL, Sizing.FILL, Sizing.HUG, 8f, loc)

        val component = ComponentNode(
            name = "PrimaryButton",
            properties = listOf(prop),
            constraints = listOf(explicitConstraint, relationalConstraint, autoLayoutConstraint),
            children = emptyList(),
            location = loc
        )

        val block = Block("buttons", listOf(component), loc)
        val importNode = Import("b", "buttons.dac", loc)
        val file = VireoFile("main.dac", listOf(importNode), blocks = listOf(block), location = loc)
        val resolvedFile = ResolvedFile(file)

        assertEquals("main.dac", file.path)
        assertEquals("test.dac", file.location.file)
        assertEquals("PrimaryButton", component.name)
        assertEquals(loc, propValueRef.location)
        assertEquals(loc, explicitConstraint.location)
        assertEquals(loc, resolvedFile.location)
    }

    @Test
    fun `test AstVisitor interface implementation`() {
        val loc = SourceLocation("test.dac", 10, 5)
        val visitor = object : AstVisitor<String> {
            override fun visitFile(node: VireoFile): String = "File:${node.path}"
            override fun visitBlock(node: Block): String = "Block:${node.name}"
            override fun visitComponent(node: ComponentNode): String = "Component:${node.name}"
            override fun visitProperty(node: Property): String = "Property:${node.key}"
            override fun visitConstraint(node: Constraint): String = "Constraint:${node::class.simpleName}"
            override fun visitVarDeclaration(node: VarDeclaration): String = "Var:${node.name}"
            override fun visitFunDeclaration(node: FunDeclaration): String = "Fun:${node.name}"
        }

        val file = VireoFile("test.dac", emptyList(), emptyList(), emptyList(), emptyList(), loc)
        val block = Block("hero", emptyList(), loc)
        val component = ComponentNode("Header", emptyList(), emptyList(), emptyList(), loc)
        val property = Property("color", PropertyValue.Literal("#FFFFFF", loc), loc)
        val constraint = Constraint.Explicit(Axis.X, 0f, loc)
        val varDecl = VarDeclaration("primaryColor", PropertyValue.Literal("#3B82F6", loc), loc)
        val funDecl = FunDeclaration("spacing", listOf(Parameter("multiplier", "Int", loc)), "Int", PropertyValue.Expr("8 * multiplier", loc), loc)
        val condExpr = PropertyValue.ConditionalExpr(PropertyValue.Expr("\$variant == \"primary\"", loc), PropertyValue.Literal("#3B82F6", loc), PropertyValue.Literal("#6B7280", loc), loc)

        assertEquals("File:test.dac", visitor.visitFile(file))
        assertEquals("Block:hero", visitor.visitBlock(block))
        assertEquals("Component:Header", visitor.visitComponent(component))
        assertEquals("Property:color", visitor.visitProperty(property))
        assertEquals("Constraint:Explicit", visitor.visitConstraint(constraint))
        assertEquals("Var:primaryColor", visitor.visitVarDeclaration(varDecl))
        assertEquals("Fun:spacing", visitor.visitFunDeclaration(funDecl))
        assertEquals(loc, condExpr.location)
    }

    @Test
    fun `test VireoResult Ok and Err`() {
        val okResult: VireoResult<String> = VireoResult.Ok("Success")
        assertTrue(okResult is VireoResult.Ok)
        val okVal = (okResult as? VireoResult.Ok)?.value
        assertEquals("Success", okVal)

        val loc = SourceLocation("test.dac", 5, 2)
        val error = VireoError("Syntax error", loc)
        val errResult: VireoResult<String> = VireoResult.Err(listOf(error))
        assertTrue(errResult is VireoResult.Err)
        val errList = (errResult as? VireoResult.Err)?.errors
        assertEquals(1, errList?.size)
        assertEquals("Syntax error", errList?.get(0)?.message)
    }

    @Test
    fun `test Renderer interface implementation`() {
        val loc = SourceLocation("test.dac", 1, 1)
        val dummyRenderer = object : Renderer<String> {
            override fun render(file: ResolvedFile): VireoResult<String> {
                return VireoResult.Ok("Rendered: ${file.file.path}")
            }
        }

        val file = VireoFile("app.dac", emptyList(), blocks = emptyList(), location = loc)
        val resolved = ResolvedFile(file)
        val result = dummyRenderer.render(resolved)

        assertTrue(result is VireoResult.Ok)
        val valResult = (result as? VireoResult.Ok)?.value
        assertEquals("Rendered: app.dac", valResult)
    }
}
