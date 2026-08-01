package com.vireo.renderer.html

import com.vireo.core.*

object HtmlRenderer : Renderer<String> {

    override fun render(file: ResolvedFile): VireoResult<String> {
        return render(file.file)
    }

    fun render(file: VireoFile): VireoResult<String> {
        return try {
            val html = buildString {
                append("<div class=\"vireo-file\" data-path=\"${escapeHtml(file.path)}\">\n")
                file.blocks.forEach { block ->
                    renderBlock(this, block, "  ")
                }
                append("</div>")
            }
            VireoResult.Ok(html)
        } catch (e: Exception) {
            VireoResult.Err(listOf(VireoError("Failed to render HTML: ${e.message}", file.location)))
        }
    }

    private fun renderBlock(builder: StringBuilder, block: Block, indent: String) {
        builder.append("$indent<div class=\"vireo-block\" data-name=\"${escapeHtml(block.name)}\">\n")
        block.components.forEach { comp ->
            renderComponent(builder, comp, "$indent  ")
        }
        builder.append("$indent</div>\n")
    }

    private fun renderComponent(builder: StringBuilder, comp: ComponentNode, indent: String) {
        val cssStyles = mutableListOf<String>()
        var textContent: String? = null

        // 1. Process constraints
        comp.constraints.forEach { constraint ->
            when (constraint) {
                is Constraint.Explicit -> {
                    val pxVal = "${constraint.value.toIntIfWhole()}px"
                    when (constraint.axis) {
                        Axis.WIDTH -> cssStyles.add("width: $pxVal;")
                        Axis.HEIGHT -> cssStyles.add("height: $pxVal;")
                        Axis.X -> {
                            cssStyles.add("position: absolute;")
                            cssStyles.add("left: $pxVal;")
                        }
                        Axis.Y -> {
                            cssStyles.add("position: absolute;")
                            cssStyles.add("top: $pxVal;")
                        }
                    }
                }
                is Constraint.Relational -> {
                    val relCss = formatRelationalCss(constraint.axis, constraint.expression)
                    if (relCss != null) {
                        cssStyles.add(relCss)
                    }
                }
                is Constraint.AutoLayout -> {
                    cssStyles.add("display: flex;")
                    val flexDir = if (constraint.direction == Direction.VERTICAL) "column" else "row"
                    cssStyles.add("flex-direction: $flexDir;")
                    if (constraint.gap > 0f) {
                        cssStyles.add("gap: ${constraint.gap.toIntIfWhole()}px;")
                    }
                    when (constraint.mainAxis) {
                        Sizing.FILL -> cssStyles.add("flex-grow: 1;")
                        Sizing.HUG -> {
                            if (constraint.direction == Direction.VERTICAL) {
                                cssStyles.add("height: fit-content;")
                            } else {
                                cssStyles.add("width: fit-content;")
                            }
                        }
                        Sizing.FIXED -> {}
                    }
                    when (constraint.crossAxis) {
                        Sizing.FILL -> cssStyles.add("align-self: stretch;")
                        Sizing.HUG -> {}
                        Sizing.FIXED -> {}
                    }
                }
            }
        }

        // 2. Process properties
        for (prop in comp.properties) {
            val key = prop.key
            val rawVal = prop.value

            val strVal = when (rawVal) {
                is PropertyValue.Literal -> rawVal.value.toString()
                is PropertyValue.Expr -> rawVal.source
                is PropertyValue.Ref -> "${rawVal.reference.file}.${rawVal.reference.block}.${rawVal.reference.component}"
                is PropertyValue.ConditionalExpr -> {
                    val condStr = when (val c = rawVal.condition) {
                        is PropertyValue.Expr -> c.source
                        is PropertyValue.Literal -> c.value.toString()
                        else -> c.toString()
                    }
                    val thenStr = when (val t = rawVal.thenBranch) {
                        is PropertyValue.Literal -> t.value.toString()
                        is PropertyValue.Expr -> t.source
                        else -> t.toString()
                    }
                    val elseStr = when (val e = rawVal.elseBranch) {
                        is PropertyValue.Literal -> e.value.toString()
                        is PropertyValue.Expr -> e.source
                        else -> e.toString()
                    }
                    "if $condStr then $thenStr else $elseStr"
                }
            }

            when (key) {
                "width" -> {
                    if (comp.constraints.none { it is Constraint.Explicit && it.axis == Axis.WIDTH } &&
                        comp.constraints.none { it is Constraint.Relational && it.axis == Axis.WIDTH }) {
                        if (strVal == "fill") {
                            cssStyles.add("width: 100%;")
                        } else if (strVal == "hug") {
                            cssStyles.add("width: fit-content;")
                        } else if (strVal.toFloatOrNull() != null) {
                            cssStyles.add("width: ${strVal.toFloat().toIntIfWhole()}px;")
                        }
                    }
                }
                "height" -> {
                    if (comp.constraints.none { it is Constraint.Explicit && it.axis == Axis.HEIGHT } &&
                        comp.constraints.none { it is Constraint.Relational && it.axis == Axis.HEIGHT }) {
                        if (strVal == "fill") {
                            cssStyles.add("height: 100%;")
                        } else if (strVal == "hug") {
                            cssStyles.add("height: fit-content;")
                        } else if (strVal.toFloatOrNull() != null) {
                            cssStyles.add("height: ${strVal.toFloat().toIntIfWhole()}px;")
                        }
                    }
                }
                "color" -> {
                    val isTextNode = comp.properties.any { it.key == "text" } ||
                            comp.name.contains("Title") || comp.name.contains("Description") ||
                            comp.name.contains("Label") || comp.name.contains("Text")
                    if (isTextNode && !comp.properties.any { it.key == "backgroundColor" }) {
                        cssStyles.add("color: $strVal;")
                    } else {
                        cssStyles.add("background-color: $strVal;")
                    }
                }
                "fontSize" -> {
                    val num = strVal.toFloatOrNull()
                    if (num != null) {
                        cssStyles.add("font-size: ${num.toIntIfWhole()}px;")
                    }
                }
                "fontWeight" -> {
                    cssStyles.add("font-weight: $strVal;")
                }
                "radius" -> {
                    val num = strVal.toFloatOrNull()
                    if (num != null) {
                        cssStyles.add("border-radius: ${num.toIntIfWhole()}px;")
                    }
                }
                "padding" -> {
                    val num = strVal.toFloatOrNull()
                    if (num != null) {
                        cssStyles.add("padding: ${num.toIntIfWhole()}px;")
                    } else {
                        val parts = strVal.split("\\s+".toRegex()).map {
                            val n = it.toFloatOrNull()
                            if (n != null) "${n.toIntIfWhole()}px" else it
                        }
                        cssStyles.add("padding: ${parts.joinToString(" ")};")
                    }
                }
                "border" -> {
                    val parts = strVal.split("\\s+".toRegex())
                    if (parts.size == 2 && parts[0].toFloatOrNull() != null) {
                        cssStyles.add("border: ${parts[0].toFloat().toIntIfWhole()}px solid ${parts[1]};")
                    } else {
                        cssStyles.add("border: $strVal;")
                    }
                }
                "shadow" -> {
                    if (strVal == "true") {
                        cssStyles.add("box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);")
                    }
                }
                "text" -> {
                    textContent = strVal
                }
                "label" -> {
                    if (textContent == null) {
                        textContent = strVal
                    }
                }
            }
        }

        val styleAttr = if (cssStyles.isNotEmpty()) {
            " style=\"${cssStyles.joinToString(" ")}\""
        } else ""

        builder.append("$indent<div class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\"$styleAttr>")

        if (textContent != null) {
            builder.append(escapeHtml(textContent))
        }

        if (comp.children.isNotEmpty()) {
            builder.append("\n")
            comp.children.forEach { child ->
                renderComponent(builder, child, "$indent  ")
            }
            builder.append(indent)
        }

        builder.append("</div>\n")
    }

    private fun formatRelationalCss(axis: Axis, expression: String): String? {
        val pctMatch = Regex("(\\d+(?:\\.\\d+)?)%parent").find(expression)
        if (pctMatch != null) {
            val pct = pctMatch.groupValues[1]
            return when (axis) {
                Axis.WIDTH -> "width: $pct%;"
                Axis.HEIGHT -> "height: $pct%;"
                Axis.X -> "left: $pct%; position: absolute;"
                Axis.Y -> "top: $pct%; position: absolute;"
            }
        }
        val parentPlusMatch = Regex("parent\\.(\\w+)\\s*([+-])\\s*(\\d+(?:\\.\\d+)?)").find(expression)
        if (parentPlusMatch != null) {
            val op = parentPlusMatch.groupValues[2]
            val valStr = parentPlusMatch.groupValues[3]
            val numVal = valStr.toFloatOrNull()?.toIntIfWhole() ?: valStr
            return when (axis) {
                Axis.WIDTH -> "width: calc(100% $op ${numVal}px);"
                Axis.HEIGHT -> "height: calc(100% $op ${numVal}px);"
                Axis.X -> "left: ${op}${numVal}px; position: absolute;"
                Axis.Y -> "top: ${op}${numVal}px; position: absolute;"
            }
        }
        return null
    }

    private fun Float.toIntIfWhole(): String {
        return if (this == this.toInt().toFloat()) {
            this.toInt().toString()
        } else {
            this.toString()
        }
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
