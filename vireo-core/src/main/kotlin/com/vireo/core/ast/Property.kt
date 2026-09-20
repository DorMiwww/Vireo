package com.vireo.core

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
