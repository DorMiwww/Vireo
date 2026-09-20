package com.vireo.core

// Helm-style reference: file.block.component
data class Reference(
    val file: String,
    val block: String,
    val component: String,
    val location: SourceLocation
)
