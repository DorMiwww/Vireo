package com.vireo.analysis

import com.vireo.core.*
import com.vireo.parser.Parser
import java.io.File

object Analyzer {

    fun analyze(
        entryFile: VireoFile,
        assetChecker: (String) -> Boolean = { File(it).exists() },
        fileLoader: (String) -> String = { File(it).readText() }
    ): VireoResult<ResolvedFile> {
        val analyzer = FileAnalyzer(entryFile, fileLoader, assetChecker)
        return analyzer.analyze()
    }
}

private class FileAnalyzer(
    private val entryFile: VireoFile,
    private val fileLoader: (String) -> String,
    private val assetChecker: (String) -> Boolean
) {
    private val loadedFiles = mutableMapOf<String, VireoFile>()
    private val visitingStack = mutableListOf<String>()
    private val errors = mutableListOf<VireoError>()

    fun analyze(): VireoResult<ResolvedFile> {
        // Step 1: Load and parse imported files recursively & detect circular imports
        loadImports(entryFile)

        // Step 2: Validate references & properties across all loaded files
        validateAllFiles()

        if (errors.isNotEmpty()) {
            return VireoResult.Err(errors)
        }

        // Step 3: Evaluate expressions on the entry file
        return when (val evalResult = ExprEvaluator.evaluate(entryFile)) {
            is VireoResult.Err -> VireoResult.Err(evalResult.errors)
            is VireoResult.Ok -> VireoResult.Ok(ResolvedFile(file = evalResult.value, loadedFiles = loadedFiles))
        }
    }

    private fun loadImports(file: VireoFile) {
        val normPath = normalizePath(file.path)
        loadedFiles[normPath] = file
        visitingStack.add(normPath)

        val seenAliases = mutableSetOf<String>()
        for (imp in file.imports) {
            if (!seenAliases.add(imp.alias)) {
                errors.add(VireoError("Duplicate import alias '${imp.alias}' in file '${file.path}'", imp.location))
            }

            val targetPath = resolvePath(file.path, imp.filePath)
            if (visitingStack.contains(targetPath)) {
                val cycleStartIndex = visitingStack.indexOf(targetPath)
                val cycleList = visitingStack.subList(cycleStartIndex, visitingStack.size) + targetPath
                val cycleStr = cycleList.joinToString(" -> ")
                errors.add(VireoError("Circular import detected: '${imp.filePath}' ($cycleStr)", imp.location))
            } else if (!loadedFiles.containsKey(targetPath)) {
                val sourceText = try {
                    fileLoader(targetPath)
                } catch (e: Exception) {
                    errors.add(VireoError("Imported file '${imp.filePath}' not found at '$targetPath': ${e.message}", imp.location))
                    null
                }

                if (sourceText != null) {
                    when (val parseResult = Parser.parse(sourceText, targetPath)) {
                        is VireoResult.Err -> {
                            errors.addAll(parseResult.errors)
                        }
                        is VireoResult.Ok -> {
                            loadImports(parseResult.value)
                        }
                    }
                }
            }
        }

        visitingStack.removeAt(visitingStack.lastIndex)
    }

    private fun validateAllFiles() {
        for (file in loadedFiles.values) {
            val visitor = ReferenceAndPropertyVisitor()
            visitor.visitFile(file)

            // Validate references
            for (refProp in visitor.references) {
                val ref = refProp.reference
                val imp = file.imports.find { it.alias == ref.file }
                if (imp == null) {
                    errors.add(VireoError("Unknown import alias '${ref.file}' in reference '${ref.file}.${ref.block}.${ref.component}'", ref.location))
                } else {
                    val targetPath = resolvePath(file.path, imp.filePath)
                    val targetFile = loadedFiles[targetPath]
                    if (targetFile != null) {
                        val targetBlock = targetFile.blocks.find { it.name == ref.block }
                        if (targetBlock == null) {
                            errors.add(VireoError("Block '${ref.block}' not found in imported file '${ref.file}' (${targetFile.path})", ref.location))
                        } else {
                            val targetComp = findComponent(targetBlock.components, ref.component)
                            if (targetComp == null) {
                                errors.add(VireoError("Component '${ref.component}' not found in block '${ref.block}' of imported file '${ref.file}' (${targetFile.path})", ref.location))
                            }
                        }
                    }
                }
            }

            // Validate property types
            for (prop in visitor.properties) {
                validatePropertyType(prop, file)
            }
        }
    }

    private fun findComponent(components: List<ComponentNode>, name: String): ComponentNode? {
        for (comp in components) {
            if (comp.name == name) return comp
            val foundInChild = findComponent(comp.children, name)
            if (foundInChild != null) return foundInChild
        }
        return null
    }

    private fun validatePropertyType(prop: Property, file: VireoFile) {
        val numericKeys = setOf("width", "height", "x", "y", "fontSize", "radius", "gap", "zIndex", "z")
        if (prop.key in numericKeys && prop.value is PropertyValue.Literal) {
            val literal = (prop.value as PropertyValue.Literal).value
            if (literal is String) {
                val isAllowedKeyword = literal.lowercase() in setOf("fill", "hug", "auto", "bold", "normal", "horizontal", "vertical", "stack", "layer")
                if (!isAllowedKeyword && literal.toFloatOrNull() == null && !literal.contains("%")) {
                    errors.add(VireoError("Property '${prop.key}' expects a numeric value or valid keyword, but got '$literal'", prop.location))
                }
            }
        } else if (prop.key == "padding" && prop.value is PropertyValue.Literal) {
            val literal = (prop.value as PropertyValue.Literal).value
            if (literal is String && literal.toFloatOrNull() == null) {
                val parts = literal.split("\\s+".toRegex())
                if (!parts.all { it.toFloatOrNull() != null }) {
                    errors.add(VireoError("Property 'padding' expects numeric space-separated values, but got '$literal'", prop.location))
                }
            }
        } else if (prop.key == "border" && prop.value is PropertyValue.Literal) {
            val literal = (prop.value as PropertyValue.Literal).value
            if (literal is String && literal.toFloatOrNull() == null) {
                val parts = literal.split("\\s+".toRegex())
                val isValidBorder = parts.size == 2 && parts[0].toFloatOrNull() != null
                if (!isValidBorder) {
                    errors.add(VireoError("Property 'border' expects width and color (e.g. '1 #D1D5DB'), but got '$literal'", prop.location))
                }
            }
        } else if (prop.key == "fit" && prop.value is PropertyValue.Literal) {
            val literal = (prop.value as PropertyValue.Literal).value.toString().lowercase().trim('"', '\'')
            val allowedFit = setOf("cover", "contain", "fill", "none")
            if (literal !in allowedFit) {
                errors.add(VireoError("Property 'fit' expects 'cover', 'contain', 'fill', or 'none', but got '$literal'", prop.location))
            }
        }

        val mediaKeys = setOf("src", "image", "backgroundImage", "poster", "video", "audio", "iframe")
        if (prop.key in mediaKeys && prop.value is PropertyValue.Literal) {
            val rawPath = (prop.value as PropertyValue.Literal).value.toString().trim('"', '\'')
            val isRemote = rawPath.startsWith("http://") || rawPath.startsWith("https://") || rawPath.startsWith("data:")
            if (!isRemote && rawPath.isNotEmpty()) {
                val cleanPath = rawPath.substringBefore("#").substringBefore("?")
                val ext = cleanPath.substringAfterLast(".", "").lowercase()
                val supportedExts = setOf(
                    // Images
                    "png", "jpg", "jpeg", "webp", "gif", "avif", "svg",
                    // Video
                    "mp4", "webm",
                    // Audio
                    "mp3", "wav", "ogg",
                    // Web / Embed
                    "html", "htm"
                )
                if (ext.isNotEmpty() && ext !in supportedExts) {
                    errors.add(VireoError("Unsupported asset format '.$ext' in property '${prop.key}' (supported: png, jpg, jpeg, webp, gif, avif, svg, mp4, webm, mp3, wav, ogg, html)", prop.location))
                } else {
                    val resolvedAssetPath = resolvePath(file.path, cleanPath)
                    if (!assetChecker(resolvedAssetPath)) {
                        errors.add(VireoError("Asset file '$rawPath' not found at '$resolvedAssetPath'", prop.location))
                    }
                }
            }
        }
    }

    private fun normalizePath(path: String): String {
        val f = File(path)
        return if (f.isAbsolute) {
            f.normalize().path
        } else {
            val normalized = f.normalize().path
            if (normalized.startsWith("./") && normalized.length > 2) {
                normalized.substring(2)
            } else {
                normalized
            }
        }
    }

    private fun resolvePath(baseFilePath: String, importPath: String): String {
        val baseFile = File(baseFilePath)
        val parentDir = baseFile.parentFile
        val resolvedFile = if (parentDir != null && parentDir.path.isNotEmpty()) {
            File(parentDir, importPath)
        } else {
            File(importPath)
        }
        return normalizePath(resolvedFile.path)
    }

    private class ReferenceAndPropertyVisitor : AstVisitor<Unit> {
        val references = mutableListOf<PropertyValue.Ref>()
        val properties = mutableListOf<Property>()

        override fun visitFile(node: VireoFile) {
            node.vars.forEach { visitVarDeclaration(it) }
            node.functions.forEach { visitFunDeclaration(it) }
            node.blocks.forEach { visitBlock(it) }
        }

        override fun visitBlock(node: Block) {
            node.components.forEach { visitComponent(it) }
        }

        override fun visitComponent(node: ComponentNode) {
            node.properties.forEach { visitProperty(it) }
            node.constraints.forEach { visitConstraint(it) }
            node.children.forEach { visitComponent(it) }
        }

        override fun visitProperty(node: Property) {
            properties.add(node)
            visitPropertyValue(node.value)
        }

        override fun visitConstraint(node: Constraint) {
            // No-op
        }

        override fun visitVarDeclaration(node: VarDeclaration) {
            visitPropertyValue(node.value)
        }

        override fun visitFunDeclaration(node: FunDeclaration) {
            visitPropertyValue(node.returnExpr)
        }

        private fun visitPropertyValue(value: PropertyValue) {
            when (value) {
                is PropertyValue.Ref -> references.add(value)
                is PropertyValue.ConditionalExpr -> {
                    visitPropertyValue(value.condition)
                    visitPropertyValue(value.thenBranch)
                    visitPropertyValue(value.elseBranch)
                }
                else -> {}
            }
        }
    }
}
