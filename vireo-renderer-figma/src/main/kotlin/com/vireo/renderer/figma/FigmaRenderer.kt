package com.vireo.renderer.figma

import com.vireo.core.*

object FigmaRenderer : Renderer<FigmaDocument> {

    override fun render(file: ResolvedFile): VireoResult<FigmaDocument> {
        return render(file.file, file.loadedFiles)
    }

    fun render(file: VireoFile, loadedFiles: Map<String, VireoFile> = emptyMap()): VireoResult<FigmaDocument> {
        return try {
            var blockCounter = 1
            val blockNodes = file.blocks.map { block ->
                renderBlock(block, blockCounter++, file, loadedFiles)
            }

            val canvasNode = FigmaNode(
                id = "0:1",
                name = file.path,
                type = "CANVAS",
                children = blockNodes
            )

            val document = FigmaDocument(
                name = file.path,
                nodes = listOf(canvasNode)
            )

            VireoResult.Ok(document)
        } catch (e: Exception) {
            VireoResult.Err(listOf(VireoError("Failed to render Figma document: ${e.message}", file.location)))
        }
    }

    private fun renderBlock(
        block: Block,
        blockId: Int,
        file: VireoFile,
        loadedFiles: Map<String, VireoFile>
    ): FigmaNode {
        var childCounter = 1
        val childrenNodes = block.components.map { comp ->
            renderComponent(comp, "1:$blockId:${childCounter++}", file, loadedFiles, "VERTICAL")
        }

        return FigmaNode(
            id = "1:$blockId",
            name = block.name,
            type = "FRAME",
            children = childrenNodes,
            layoutMode = "VERTICAL",
            primaryAxisSizingMode = "AUTO",
            counterAxisSizingMode = "AUTO",
            itemSpacing = 16f
        )
    }

    private fun renderComponent(
        rawComp: ComponentNode,
        nodeId: String,
        file: VireoFile,
        loadedFiles: Map<String, VireoFile>,
        parentLayoutDir: String? = null
    ): FigmaNode {
        val comp = resolveComponentRef(rawComp, file, loadedFiles)

        var textContent: String? = null
        var placeholderText: String? = null
        var colorHex: String? = null
        var fontSizeVal: Float? = null
        var fontWeightVal: Float? = null
        var radiusVal: Float? = null
        var pLeft: Float? = null
        var pRight: Float? = null
        var pTop: Float? = null
        var pBottom: Float? = null
        var strokeW: Float? = null
        var strokeHex: String? = null

        var widthVal: Float? = null
        var heightVal: Float? = null
        var xVal: Float? = null
        var yVal: Float? = null
        var isFillWidth = false
        var isHugWidth = false
        var isFillHeight = false
        var isHugHeight = false

        var layoutDir: String? = null
        var mainAxisSizing: String? = null
        var crossAxisSizing: String? = null
        var itemGap: Float? = null
        var primaryAlign: String? = null
        var counterAlign: String? = null

        // 1. Process constraints
        comp.constraints.forEach { constraint ->
            when (constraint) {
                is Constraint.Explicit -> {
                    when (constraint.axis) {
                        Axis.WIDTH -> widthVal = constraint.value
                        Axis.HEIGHT -> heightVal = constraint.value
                        Axis.X -> xVal = constraint.value
                        Axis.Y -> yVal = constraint.value
                    }
                }
                is Constraint.Relational -> {
                    val pctMatch = Regex("(\\d+(?:\\.\\d+)?)%parent").find(constraint.expression)
                    if (pctMatch != null) {
                        val pct = pctMatch.groupValues[1].toFloatOrNull() ?: 100f
                        if (constraint.axis == Axis.WIDTH) {
                            widthVal = pct
                        } else if (constraint.axis == Axis.HEIGHT) {
                            heightVal = pct
                        }
                    }
                }
                is Constraint.AutoLayout -> {
                    layoutDir = if (constraint.direction == Direction.VERTICAL) "VERTICAL" else "HORIZONTAL"
                    itemGap = constraint.gap
                    mainAxisSizing = when (constraint.mainAxis) {
                        Sizing.FILL, Sizing.FIXED -> "FIXED"
                        Sizing.HUG -> "AUTO"
                    }
                    crossAxisSizing = when (constraint.crossAxis) {
                        Sizing.FILL, Sizing.FIXED -> "FIXED"
                        Sizing.HUG -> "AUTO"
                    }
                }
            }
        }

        // 2. Process properties
        comp.properties.forEach { prop ->
            val key = prop.key
            val rawVal = prop.value
            val strVal = extractPropertyValueString(rawVal)

            when (key) {
                "width" -> {
                    when (strVal) {
                        "fill" -> isFillWidth = true
                        "hug" -> isHugWidth = true
                        else -> strVal.toFloatOrNull()?.let { widthVal = it }
                    }
                }
                "height" -> {
                    when (strVal) {
                        "fill" -> isFillHeight = true
                        "hug" -> isHugHeight = true
                        else -> strVal.toFloatOrNull()?.let { heightVal = it }
                    }
                }
                "color" -> colorHex = strVal
                "fontSize" -> strVal.toFloatOrNull()?.let { fontSizeVal = it }
                "fontWeight" -> {
                    fontWeightVal = when (strVal.lowercase()) {
                        "bold" -> 700f
                        "medium" -> 500f
                        "regular" -> 400f
                        "light" -> 300f
                        else -> strVal.toFloatOrNull() ?: 400f
                    }
                }
                "radius" -> strVal.toFloatOrNull()?.let { radiusVal = it }
                "padding" -> {
                    val parts = strVal.split("\\s+".toRegex()).mapNotNull { it.toFloatOrNull() }
                    when (parts.size) {
                        1 -> {
                            pLeft = parts[0]; pRight = parts[0]; pTop = parts[0]; pBottom = parts[0]
                        }
                        2 -> {
                            pTop = parts[0]; pBottom = parts[0]; pLeft = parts[1]; pRight = parts[1]
                        }
                        4 -> {
                            pTop = parts[0]; pRight = parts[1]; pBottom = parts[2]; pLeft = parts[3]
                        }
                    }
                }
                "backgroundColor" -> colorHex = strVal
                "alignItems" -> {
                    counterAlign = when (strVal.lowercase().trim('"', '\'')) {
                        "center" -> "CENTER"
                        "flex-start", "start", "min" -> "MIN"
                        "flex-end", "end", "max" -> "MAX"
                        else -> null
                    }
                }
                "justifyContent" -> {
                    primaryAlign = when (strVal.lowercase().trim('"', '\'')) {
                        "center" -> "CENTER"
                        "space-between" -> "SPACE_BETWEEN"
                        "flex-start", "start", "min" -> "MIN"
                        "flex-end", "end", "max" -> "MAX"
                        else -> null
                    }
                }
                "border" -> {
                    val parts = strVal.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        parts[0].toFloatOrNull()?.let { strokeW = it }
                        strokeHex = parts[1]
                    }
                }
                "placeholder" -> {
                    placeholderText = strVal
                }
                "text", "label" -> {
                    if (textContent == null) {
                        textContent = strVal
                    }
                }
            }
        }

        val isInputBox = placeholderText != null || (textContent != null && (radiusVal != null || strokeW != null || strokeHex != null))
        val isButtonOrContainerWithChildren = comp.children.isNotEmpty() || layoutDir != null

        val nodeType = if (isInputBox || isButtonOrContainerWithChildren) {
            "FRAME"
        } else if (textContent != null) {
            "TEXT"
        } else {
            "RECTANGLE"
        }

        // Auto Layout defaults for containers
        var effectiveLayoutDir = layoutDir
        var effectivePrimaryAlign = primaryAlign
        var effectiveCounterAlign = counterAlign

        if (isInputBox && effectiveLayoutDir == null) {
            effectiveLayoutDir = "HORIZONTAL"
            if (effectiveCounterAlign == null) effectiveCounterAlign = "CENTER"
            if (pLeft == null) pLeft = 16f
            if (pRight == null) pRight = 16f
            if (heightVal == null) heightVal = 44f
        } else if (comp.children.isNotEmpty() && effectiveLayoutDir == null) {
            // Button-like container centering its children
            effectiveLayoutDir = "HORIZONTAL"
            if (effectivePrimaryAlign == null) effectivePrimaryAlign = "CENTER"
            if (effectiveCounterAlign == null) effectiveCounterAlign = "CENTER"
            if (heightVal == null) heightVal = 44f
        }

        // Sizing in Auto Layout
        val isVertical = effectiveLayoutDir == "VERTICAL"
        val isHorizontal = effectiveLayoutDir == "HORIZONTAL"

        val primarySizing = when {
            isVertical -> when {
                heightVal != null -> "FIXED"
                isFillHeight || mainAxisSizing == "FIXED" -> "FIXED"
                isHugHeight || mainAxisSizing == "AUTO" -> "AUTO"
                else -> "AUTO"
            }
            isHorizontal -> when {
                widthVal != null -> "FIXED"
                isFillWidth || mainAxisSizing == "FIXED" -> "FIXED"
                isHugWidth || mainAxisSizing == "AUTO" -> "AUTO"
                else -> "AUTO"
            }
            else -> null
        }

        val counterSizing = when {
            isVertical -> when {
                widthVal != null -> "FIXED"
                isFillWidth || crossAxisSizing == "FIXED" -> "FIXED"
                isHugWidth || crossAxisSizing == "AUTO" -> "AUTO"
                else -> "AUTO"
            }
            isHorizontal -> when {
                heightVal != null -> "FIXED"
                isFillHeight || crossAxisSizing == "FIXED" -> "FIXED"
                isHugHeight || crossAxisSizing == "AUTO" -> "AUTO"
                else -> "AUTO"
            }
            else -> null
        }

        val layoutAlignVal = when (parentLayoutDir) {
            "VERTICAL" -> if (isFillWidth) "STRETCH" else null
            "HORIZONTAL" -> if (isFillHeight) "STRETCH" else null
            else -> if (isFillWidth) "STRETCH" else null
        }

        val layoutGrowVal = when (parentLayoutDir) {
            "VERTICAL" -> if (isFillHeight) 1f else null
            "HORIZONTAL" -> if (isFillWidth) 1f else null
            else -> null
        }

        val fillsList = colorHex?.let { listOf(Paint(type = "SOLID", color = Color.fromHex(it))) }
        val strokesList = strokeHex?.let { listOf(Paint(type = "SOLID", color = Color.fromHex(it))) }

        val typeStyle = if (fontSizeVal != null || fontWeightVal != null) {
            TypeStyle(fontSize = fontSizeVal, fontWeight = fontWeightVal)
        } else null

        // Render children
        var childCounter = 1
        var renderedChildren: List<FigmaNode>? = if (isInputBox && placeholderText != null && comp.children.isEmpty()) {
            listOf(
                FigmaNode(
                    id = "$nodeId:placeholder",
                    name = "Placeholder",
                    type = "TEXT",
                    characters = placeholderText,
                    style = TypeStyle(fontSize = fontSizeVal ?: 14f),
                    fills = listOf(Paint(type = "SOLID", color = Color.fromHex("#9CA3AF")))
                )
            )
        } else if (comp.children.isNotEmpty()) {
            comp.children.map { child ->
                renderComponent(child, "$nodeId:${childCounter++}", file, loadedFiles, effectiveLayoutDir)
            }
        } else null

        val boundingBox = if (xVal != null || yVal != null || widthVal != null || heightVal != null) {
            Rect(
                x = xVal ?: 0f,
                y = yVal ?: 0f,
                width = widthVal ?: 0f,
                height = heightVal ?: 0f
            )
        } else null

        return FigmaNode(
            id = nodeId,
            name = comp.name,
            type = nodeType,
            children = renderedChildren,
            characters = if (nodeType == "TEXT") textContent else null,
            style = typeStyle,
            fills = fillsList ?: (if (isInputBox) listOf(Paint(type = "SOLID", color = Color(1f, 1f, 1f))) else null),
            strokes = strokesList,
            strokeWeight = strokeW,
            cornerRadius = radiusVal,
            layoutMode = effectiveLayoutDir,
            primaryAxisSizingMode = primarySizing,
            counterAxisSizingMode = counterSizing,
            primaryAxisAlignItems = effectivePrimaryAlign,
            counterAxisAlignItems = effectiveCounterAlign,
            layoutAlign = layoutAlignVal,
            layoutGrow = layoutGrowVal,
            itemSpacing = itemGap,
            paddingLeft = pLeft,
            paddingRight = pRight,
            paddingTop = pTop,
            paddingBottom = pBottom,
            absoluteBoundingBox = boundingBox
        )
    }

    private fun resolveComponentRef(
        comp: ComponentNode,
        sourceFile: VireoFile,
        loadedFiles: Map<String, VireoFile>
    ): ComponentNode {
        val refProp = comp.properties.find { it.key == "ref" } ?: return comp
        if (refProp.value !is PropertyValue.Ref) return comp

        val ref = (refProp.value as PropertyValue.Ref).reference
        val imp = sourceFile.imports.find { it.alias == ref.file } ?: return comp
        val targetPath = resolvePath(sourceFile.path, imp.filePath)
        val targetFile = loadedFiles[targetPath] ?: return comp
        val targetBlock = targetFile.blocks.find { it.name == ref.block } ?: return comp
        val baseComp = findComponent(targetBlock.components, ref.component) ?: return comp

        // Inherit properties, with comp overriding
        val overriddenKeys = comp.properties.map { it.key }.toSet()
        val mergedProps = comp.properties.filter { it.key != "ref" } +
            baseComp.properties.filter { it.key !in overriddenKeys && it.key != "ref" }

        val overriddenAxes = comp.constraints.map {
            when (it) {
                is Constraint.Explicit -> it.axis
                is Constraint.Relational -> it.axis
                is Constraint.AutoLayout -> Axis.WIDTH
            }
        }.toMutableSet()
        if (comp.properties.any { it.key == "width" }) overriddenAxes.add(Axis.WIDTH)
        if (comp.properties.any { it.key == "height" }) overriddenAxes.add(Axis.HEIGHT)
        val mergedConstraints = comp.constraints +
            baseComp.constraints.filter {
                val axis = when (it) {
                    is Constraint.Explicit -> it.axis
                    is Constraint.Relational -> it.axis
                    is Constraint.AutoLayout -> Axis.WIDTH
                }
                axis !in overriddenAxes
            }

        // Label override
        val customLabel = comp.properties.find { it.key == "label" }?.let { extractPropertyValueString(it.value) }
        val mergedChildren = if (comp.children.isNotEmpty()) {
            comp.children
        } else if (customLabel != null && baseComp.children.isNotEmpty()) {
            baseComp.children.map { child ->
                if (child.name == "Label" || child.properties.any { it.key == "text" }) {
                    val updatedProps = child.properties.filter { it.key != "text" } +
                        Property("text", PropertyValue.Literal(customLabel, child.location), child.location)
                    child.copy(properties = updatedProps)
                } else {
                    child
                }
            }
        } else {
            baseComp.children
        }

        return comp.copy(
            constraints = mergedConstraints,
            properties = mergedProps,
            children = mergedChildren
        )
    }

    private fun findComponent(components: List<ComponentNode>, name: String): ComponentNode? {
        for (c in components) {
            if (c.name == name) return c
            val found = findComponent(c.children, name)
            if (found != null) return found
        }
        return null
    }

    private fun resolvePath(basePath: String, relativePath: String): String {
        val cleanRel = relativePath.trimStart('.', '/')
        val baseDir = basePath.substringBeforeLast('/', "")
        val combined = if (baseDir.isEmpty()) cleanRel else "$baseDir/$cleanRel"
        val parts = combined.split('/')
        val normalized = mutableListOf<String>()
        for (part in parts) {
            when (part) {
                "", "." -> {}
                ".." -> if (normalized.isNotEmpty()) normalized.removeAt(normalized.lastIndex)
                else -> normalized.add(part)
            }
        }
        return normalized.joinToString("/")
    }

    private fun extractPropertyValueString(rawVal: PropertyValue): String {
        return when (rawVal) {
            is PropertyValue.Literal -> rawVal.value.toString()
            is PropertyValue.Expr -> rawVal.source
            is PropertyValue.Ref -> "${rawVal.reference.file}.${rawVal.reference.block}.${rawVal.reference.component}"
            is PropertyValue.ConditionalExpr -> {
                val condStr = extractPropertyValueString(rawVal.condition)
                val thenStr = extractPropertyValueString(rawVal.thenBranch)
                val elseStr = extractPropertyValueString(rawVal.elseBranch)
                "if $condStr then $thenStr else $elseStr"
            }
        }
    }
}
