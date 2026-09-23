package com.vireo.renderer.figma

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class FigmaDocument(
    @EncodeDefault val schemaVersion: Int = SCHEMA_VERSION,
    val name: String,
    val nodes: List<FigmaNode> = emptyList()
) {
    companion object {
        // Bump when the *.figma.json node shape changes in a way the Figma plugin's
        // code.js must also handle — see SUPPORTED_SCHEMA_VERSION there.
        const val SCHEMA_VERSION: Int = 2
    }

    fun toJson(pretty: Boolean = true): String {
        val jsonFormatter = Json {
            prettyPrint = pretty
            encodeDefaults = false
            ignoreUnknownKeys = true
        }
        return jsonFormatter.encodeToString(serializer(), this)
    }
}

@Serializable
data class FigmaNode(
    val id: String,
    val name: String,
    val type: String,
    val children: List<FigmaNode>? = null,
    val characters: String? = null,
    val style: TypeStyle? = null,
    val fills: List<Paint>? = null,
    val strokes: List<Paint>? = null,
    val strokeWeight: Float? = null,
    val cornerRadius: Float? = null,
    val layoutMode: String? = null,
    val primaryAxisSizingMode: String? = null,
    val counterAxisSizingMode: String? = null,
    val primaryAxisAlignItems: String? = null,
    val counterAxisAlignItems: String? = null,
    val layoutAlign: String? = null,
    val layoutGrow: Float? = null,
    val itemSpacing: Float? = null,
    val paddingLeft: Float? = null,
    val paddingRight: Float? = null,
    val paddingTop: Float? = null,
    val paddingBottom: Float? = null,
    val absoluteBoundingBox: Rect? = null,
    val mediaType: String? = null,
    val mediaUrl: String? = null,
    val imageBase64: String? = null,
    val svgContent: String? = null,
    val scaleMode: String? = null
)

@Serializable
data class Rect(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

@Serializable
data class TypeStyle(
    val fontSize: Float? = null,
    val fontWeight: Float? = null,
    val fontFamily: String? = null
)

@Serializable
data class Paint(
    val type: String = "SOLID",
    val color: Color? = null,
    val opacity: Float? = 1.0f,
    val scaleMode: String? = null,
    val imageRef: String? = null
)

@Serializable
data class Color(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1.0f
) {
    companion object {
        fun fromHex(hex: String): Color {
            val cleanHex = hex.removePrefix("#").trim()
            return when (cleanHex.length) {
                6 -> {
                    val r = cleanHex.substring(0, 2).toInt(16) / 255.0f
                    val g = cleanHex.substring(2, 4).toInt(16) / 255.0f
                    val b = cleanHex.substring(4, 6).toInt(16) / 255.0f
                    Color(r, g, b, 1.0f)
                }
                8 -> {
                    val r = cleanHex.substring(0, 2).toInt(16) / 255.0f
                    val g = cleanHex.substring(2, 4).toInt(16) / 255.0f
                    val b = cleanHex.substring(4, 6).toInt(16) / 255.0f
                    val a = cleanHex.substring(6, 8).toInt(16) / 255.0f
                    Color(r, g, b, a)
                }
                3 -> {
                    val r = "${cleanHex[0]}${cleanHex[0]}".toInt(16) / 255.0f
                    val g = "${cleanHex[1]}${cleanHex[1]}".toInt(16) / 255.0f
                    val b = "${cleanHex[2]}${cleanHex[2]}".toInt(16) / 255.0f
                    Color(r, g, b, 1.0f)
                }
                else -> Color(0f, 0f, 0f, 1f)
            }
        }
    }
}
