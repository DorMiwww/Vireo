package com.vireo.core

data class SourceLocation(
    val file: String,
    val line: Int,
    val column: Int
)

data class VireoError(
    val message: String,
    val location: SourceLocation
)

sealed class VireoResult<out T> {
    data class Ok<out T>(val value: T) : VireoResult<T>()
    data class Err(val errors: List<VireoError>) : VireoResult<Nothing>()
}
