package com.vireo.renderer.json

import com.vireo.core.*

object JsonRenderer : Renderer<String> {

    override fun render(file: ResolvedFile): VireoResult<String> {
        return render(file.file)
    }

    fun render(file: VireoFile): VireoResult<String> {
        return try {
            val json = buildString {
                append("{\n")
                if (file.imports.isNotEmpty()) {
                    append("  \"imports\": [\n")
                    file.imports.forEachIndexed { index, imp ->
                        append("    {\n")
                        append("      \"alias\": \"${escapeJson(imp.alias)}\",\n")
                        append("      \"filePath\": \"${escapeJson(imp.filePath)}\"\n")
                        append("    }${if (index < file.imports.size - 1) "," else ""}\n")
                    }
                    append("  ],\n")
                }
                if (file.vars.isNotEmpty()) {
                    append("  \"vars\": [\n")
                    file.vars.forEachIndexed { index, v ->
                        append("    {\n")
                        append("      \"name\": \"${escapeJson(v.name)}\",\n")
                        append("      \"value\": ")
                        renderPropertyValue(this, v.value)
                        append("\n    }${if (index < file.vars.size - 1) "," else ""}\n")
                    }
                    append("  ],\n")
                }
                if (file.functions.isNotEmpty()) {
                    append("  \"functions\": [\n")
                    file.functions.forEachIndexed { index, fn ->
                        append("    {\n")
                        append("      \"name\": \"${escapeJson(fn.name)}\",\n")
                        append("      \"returnType\": \"${escapeJson(fn.returnType)}\"\n")
                        append("    }${if (index < file.functions.size - 1) "," else ""}\n")
                    }
                    append("  ],\n")
                }
                append("  \"blocks\": [\n")
                file.blocks.forEachIndexed { blockIdx, block ->
                    renderBlock(this, block, "    ")
                    if (blockIdx < file.blocks.size - 1) append(",")
                    append("\n")
                }
                append("  ]\n")
                append("}")
            }
            VireoResult.Ok(json)
        } catch (e: Exception) {
            VireoResult.Err(listOf(VireoError("Failed to render JSON: ${e.message}", file.location)))
        }
    }

    private fun renderBlock(builder: StringBuilder, block: Block, indent: String) {
        builder.append("$indent{\n")
        builder.append("$indent  \"name\": \"${escapeJson(block.name)}\",\n")
        builder.append("$indent  \"components\": [\n")
        block.components.forEachIndexed { compIdx, comp ->
            renderComponent(builder, comp, "$indent    ")
            if (compIdx < block.components.size - 1) builder.append(",")
            builder.append("\n")
        }
        builder.append("$indent  ]\n")
        builder.append("$indent}")
    }

    private fun renderComponent(builder: StringBuilder, comp: ComponentNode, indent: String) {
        builder.append("$indent{\n")
        builder.append("$indent  \"name\": \"${escapeJson(comp.name)}\"")
        if (comp.properties.isNotEmpty()) {
            builder.append(",\n")
            builder.append("$indent  \"properties\": {\n")
            comp.properties.forEachIndexed { propIdx, prop ->
                builder.append("$indent    \"${escapeJson(prop.key)}\": ")
                renderPropertyValue(builder, prop.value)
                if (propIdx < comp.properties.size - 1) builder.append(",")
                builder.append("\n")
            }
            builder.append("$indent  }")
        }
        if (comp.children.isNotEmpty()) {
            builder.append(",\n")
            builder.append("$indent  \"children\": [\n")
            comp.children.forEachIndexed { childIdx, child ->
                renderComponent(builder, child, "$indent    ")
                if (childIdx < comp.children.size - 1) builder.append(",")
                builder.append("\n")
            }
            builder.append("$indent  ]")
        }
        builder.append("\n$indent}")
    }

    private fun renderPropertyValue(builder: StringBuilder, value: PropertyValue) {
        when (value) {
            is PropertyValue.Literal -> {
                when (val v = value.value) {
                    is Number -> builder.append(v)
                    is Boolean -> builder.append(v)
                    else -> builder.append("\"${escapeJson(v.toString())}\"")
                }
            }
            is PropertyValue.Ref -> {
                val ref = value.reference
                val refStr = "${ref.file}.${ref.block}.${ref.component}"
                builder.append("\"ref: ${escapeJson(refStr)}\"")
            }
            is PropertyValue.Expr -> {
                builder.append("\"${escapeJson(value.source)}\"")
            }
            is PropertyValue.ConditionalExpr -> {
                builder.append("{\n")
                builder.append("  \"type\": \"conditional\",\n")
                builder.append("  \"condition\": ")
                renderPropertyValue(builder, value.condition)
                builder.append(",\n  \"then\": ")
                renderPropertyValue(builder, value.thenBranch)
                builder.append(",\n  \"else\": ")
                renderPropertyValue(builder, value.elseBranch)
                builder.append("\n}")
            }
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
