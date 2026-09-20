package com.vireo.core

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

data class ResolvedFile(
    val file: VireoFile,
    val location: SourceLocation = file.location,
    val loadedFiles: Map<String, VireoFile> = emptyMap()
)
