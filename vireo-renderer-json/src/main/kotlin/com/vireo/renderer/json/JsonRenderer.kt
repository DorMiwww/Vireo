package com.vireo.renderer.json

import com.vireo.core.*
import kotlinx.serialization.json.*

object JsonRenderer : Renderer<String> {

    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    override fun render(file: ResolvedFile): VireoResult<String> = render(file.file)

    fun render(file: VireoFile): VireoResult<String> {
        return try {
            VireoResult.Ok(json.encodeToString(JsonElement.serializer(), buildFileJson(file)))
        } catch (e: Exception) {
            VireoResult.Err(listOf(VireoError("Failed to render JSON: ${e.message}", file.location)))
        }
    }

    private fun buildFileJson(file: VireoFile): JsonObject = buildJsonObject {
        if (file.imports.isNotEmpty()) {
            putJsonArray("imports") {
                file.imports.forEach { imp ->
                    addJsonObject {
                        put("alias", imp.alias)
                        put("filePath", imp.filePath)
                    }
                }
            }
        }
        if (file.vars.isNotEmpty()) {
            putJsonArray("vars") {
                file.vars.forEach { v ->
                    addJsonObject {
                        put("name", v.name)
                        put("value", propertyValueToJson(v.value))
                    }
                }
            }
        }
        if (file.functions.isNotEmpty()) {
            putJsonArray("functions") {
                file.functions.forEach { fn ->
                    addJsonObject {
                        put("name", fn.name)
                        put("returnType", fn.returnType)
                    }
                }
            }
        }
        putJsonArray("blocks") {
            file.blocks.forEach { add(buildBlockJson(it)) }
        }
    }

    private fun buildBlockJson(block: Block): JsonObject = buildJsonObject {
        put("name", block.name)
        putJsonArray("components") {
            block.components.forEach { add(buildComponentJson(it)) }
        }
    }

    private fun buildComponentJson(comp: ComponentNode): JsonObject = buildJsonObject {
        put("name", comp.name)
        if (comp.properties.isNotEmpty()) {
            putJsonObject("properties") {
                comp.properties.forEach { prop ->
                    put(prop.key, propertyValueToJson(prop.value))
                }
            }
        }
        if (comp.children.isNotEmpty()) {
            putJsonArray("children") {
                comp.children.forEach { add(buildComponentJson(it)) }
            }
        }
    }

    private fun propertyValueToJson(value: PropertyValue): JsonElement = when (value) {
        is PropertyValue.Literal -> when (val v = value.value) {
            is Int -> JsonPrimitive(v)
            is Long -> JsonPrimitive(v)
            is Double -> JsonPrimitive(v)
            is Float -> JsonPrimitive(v)
            is Number -> JsonPrimitive(v.toDouble())
            is Boolean -> JsonPrimitive(v)
            else -> JsonPrimitive(v.toString())
        }
        is PropertyValue.Ref -> {
            val ref = value.reference
            JsonPrimitive("ref: ${ref.file}.${ref.block}.${ref.component}")
        }
        is PropertyValue.Expr -> JsonPrimitive(value.source)
        is PropertyValue.ConditionalExpr -> buildJsonObject {
            put("type", "conditional")
            put("condition", propertyValueToJson(value.condition))
            put("then", propertyValueToJson(value.thenBranch))
            put("else", propertyValueToJson(value.elseBranch))
        }
    }
}
