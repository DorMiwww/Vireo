package com.vireo.core

data class VarDeclaration(
    val name: String,
    val value: PropertyValue,
    val location: SourceLocation
)

data class Parameter(
    val name: String,
    val type: String,
    val location: SourceLocation
)

data class FunDeclaration(
    val name: String,
    val parameters: List<Parameter>,
    val returnType: String,
    val returnExpr: PropertyValue,
    val location: SourceLocation
)

data class VireoFile(
    val path: String,
    val imports: List<Import>,
    val vars: List<VarDeclaration> = emptyList(),
    val functions: List<FunDeclaration> = emptyList(),
    val blocks: List<Block>,
    val location: SourceLocation
)

data class Import(
    val alias: String,
    val filePath: String,
    val location: SourceLocation
)

data class Block(
    val name: String,
    val components: List<ComponentNode>,
    val location: SourceLocation
)

data class ComponentNode(
    val name: String,
    val properties: List<Property>,
    val constraints: List<Constraint>,
    val children: List<ComponentNode>,
    val location: SourceLocation
)

data class Reference(
    val file: String,
    val block: String,
    val component: String,
    val location: SourceLocation
)

sealed class PropertyValue {
    abstract val location: SourceLocation

    data class Literal(
        val value: Any,
        override val location: SourceLocation
    ) : PropertyValue()

    data class Ref(
        val reference: Reference,
        override val location: SourceLocation = reference.location
    ) : PropertyValue()

    data class Expr(
        val source: String,
        override val location: SourceLocation
    ) : PropertyValue()

    data class ConditionalExpr(
        val condition: PropertyValue,
        val thenBranch: PropertyValue,
        val elseBranch: PropertyValue,
        override val location: SourceLocation
    ) : PropertyValue()
}

data class Property(
    val key: String,
    val value: PropertyValue,
    val location: SourceLocation
)

sealed class Constraint {
    abstract val location: SourceLocation

    data class Explicit(
        val axis: Axis,
        val value: Float,
        override val location: SourceLocation
    ) : Constraint()

    data class Relational(
        val axis: Axis,
        val expression: String,
        override val location: SourceLocation
    ) : Constraint()

    data class AutoLayout(
        val direction: Direction,
        val mainAxis: Sizing,
        val crossAxis: Sizing,
        val gap: Float = 0f,
        override val location: SourceLocation
    ) : Constraint()
}

enum class Axis {
    WIDTH, HEIGHT, X, Y
}

enum class Direction {
    HORIZONTAL, VERTICAL
}

enum class Sizing {
    FILL, HUG, FIXED
}

data class ResolvedFile(
    val file: VireoFile,
    val location: SourceLocation = file.location,
    val loadedFiles: Map<String, VireoFile> = emptyMap()
)
