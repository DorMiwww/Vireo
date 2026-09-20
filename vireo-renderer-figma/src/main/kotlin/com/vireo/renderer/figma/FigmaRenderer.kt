package com.vireo.renderer.figma

import com.vireo.core.*

object FigmaRenderer : Renderer<FigmaDocument> {

    override fun render(file: ResolvedFile): VireoResult<FigmaDocument> {
        return render(file.file)
    }

    fun render(file: VireoFile): VireoResult<FigmaDocument> {
        return try {
            var blockCounter = 1
            val blockNodes = file.blocks.map { block ->
                renderBlock(block, blockCounter++)
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

    private fun renderBlock(block: Block, blockId: Int): FigmaNode {
        var childCounter = 1
        val childrenNodes = block.components.map { comp ->
            renderComponent(comp, "1:$blockId:${childCounter++}")
        }

        return FigmaNode(
            id = "1:$blockId",
            name = block.name,
            type = "FRAME",
            children = childrenNodes,
            layoutMode = "VERTICAL",
            itemSpacing = 16f
        )
    }

    private fun renderComponent(comp: ComponentNode, nodeId: String): FigmaNode {
        var textContent: String? = null
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
                "border" -> {
                    val parts = strVal.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        parts[0].toFloatOrNull()?.let { strokeW = it }
                        strokeHex = parts[1]
                    }
                }
                "text", "label", "placeholder" -> {
                    if (textContent == null) {
                        textContent = strVal
                    }
                }
            }
        }

        val isTextNode = textContent != null && comp.children.isEmpty()
        val nodeType = if (isTextNode) "TEXT" else if (comp.children.isNotEmpty() || layoutDir != null) "FRAME" else "RECTANGLE"

        val fillsList = colorHex?.let { listOf(Paint(type = "SOLID", color = Color.fromHex(it))) }
        val strokesList = strokeHex?.let { listOf(Paint(type = "SOLID", color = Color.fromHex(it))) }

        val typeStyle = if (fontSizeVal != null || fontWeightVal != null) {
            TypeStyle(fontSize = fontSizeVal, fontWeight = fontWeightVal)
        } else null

        val primarySizing = when {
            isHugWidth || mainAxisSizing == "AUTO" -> "AUTO"
            isFillWidth || mainAxisSizing == "FIXED" -> "FIXED"
            else -> if (widthVal != null) "FIXED" else null
        }

        val counterSizing = when {
            isHugHeight || crossAxisSizing == "AUTO" -> "AUTO"
            isFillHeight || crossAxisSizing == "FIXED" -> "FIXED"
            else -> if (heightVal != null) "FIXED" else null
        }

        var childCounter = 1
        val renderedChildren = comp.children.map { child ->
            renderComponent(child, "$nodeId:${childCounter++}")
        }.ifEmpty { null }

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
            characters = textContent,
            style = typeStyle,
            fills = fillsList,
            strokes = strokesList,
            strokeWeight = strokeW,
            cornerRadius = radiusVal,
            layoutMode = layoutDir,
            primaryAxisSizingMode = primarySizing,
            counterAxisSizingMode = counterSizing,
            itemSpacing = itemGap,
            paddingLeft = pLeft,
            paddingRight = pRight,
            paddingTop = pTop,
            paddingBottom = pBottom,
            absoluteBoundingBox = boundingBox
        )
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
