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
