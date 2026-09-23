package com.vireo.core

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
    HORIZONTAL, VERTICAL, STACK
}

enum class Sizing {
    FILL, HUG, FIXED
}
