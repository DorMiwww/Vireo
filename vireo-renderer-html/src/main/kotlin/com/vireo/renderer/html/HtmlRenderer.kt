package com.vireo.renderer.html

import com.vireo.core.*

data class HtmlRenderOptions(
    val standardHtml: Boolean = true,
    val title: String? = null,
    val theme: String = "light",
    val assetResolver: (String) -> String = { it }
)

object HtmlRenderer : Renderer<String> {

    override fun render(file: ResolvedFile): VireoResult<String> {
        return render(file, HtmlRenderOptions())
    }

    fun render(file: ResolvedFile, options: HtmlRenderOptions): VireoResult<String> {
        return render(file.file, file.loadedFiles, options)
    }

    fun render(
        file: VireoFile,
        loadedFiles: Map<String, VireoFile> = emptyMap(),
        options: HtmlRenderOptions = HtmlRenderOptions()
    ): VireoResult<String> {
        return try {
            val pageTitle = options.title ?: file.path
            val bgColor = when (options.theme.lowercase()) {
                "dark" -> "#111827"
                else -> "#F3F4F6"
            }

            val html = buildString {
                if (options.standardHtml) {
                    append("<!DOCTYPE html>\n")
                    append("<html lang=\"en\">\n")
                    append("<head>\n")
                    append("  <meta charset=\"UTF-8\">\n")
                    append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                    append("  <title>${escapeHtml(pageTitle)}</title>\n")
                    append("  <style>\n")
                    append("    * { box-sizing: border-box; }\n")
                    append("    a.vireo-component { text-decoration: none; }\n")
                    append("    a.vireo-component:hover { opacity: 0.92; filter: brightness(1.05); }\n")
                    append("  </style>\n")
                    append("</head>\n")
                    append("<body style=\"margin: 0; padding: 0;\">\n")
                    append("  <div class=\"vireo-file\" data-path=\"${escapeHtml(file.path)}\" style=\"font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; background-color: $bgColor; margin: 0; box-sizing: border-box;\">\n")
                    file.blocks.forEach { block ->
                        renderBlock(this, block, "    ", file, loadedFiles, options)
                    }
                    append("  </div>\n")
                    append("</body>\n")
                    append("</html>")
                } else {
                    append("<div class=\"vireo-file\" data-path=\"${escapeHtml(file.path)}\" style=\"font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; box-sizing: border-box;\">\n")
                    file.blocks.forEach { block ->
                        renderBlock(this, block, "  ", file, loadedFiles, options)
                    }
                    append("</div>")
                }
            }
            VireoResult.Ok(html)
        } catch (e: Exception) {
            VireoResult.Err(listOf(VireoError("Failed to render HTML: ${e.message}", file.location)))
        }
    }

    private fun renderBlock(
        builder: StringBuilder,
        block: Block,
        indent: String,
        file: VireoFile,
        loadedFiles: Map<String, VireoFile>,
        options: HtmlRenderOptions
    ) {
        builder.append("$indent<div class=\"vireo-block\" data-name=\"${escapeHtml(block.name)}\">\n")
        block.components.forEach { comp ->
            renderComponent(builder, comp, "$indent  ", file, loadedFiles, options, "VERTICAL")
        }
        builder.append("$indent</div>\n")
    }

    private fun renderComponent(
        builder: StringBuilder,
        rawComp: ComponentNode,
        indent: String,
        file: VireoFile,
        loadedFiles: Map<String, VireoFile>,
        options: HtmlRenderOptions,
        parentLayoutDir: String? = null
    ) {
        val comp = resolveComponentRef(rawComp, file, loadedFiles)
        val cssStyles = mutableListOf<String>()
        cssStyles.add("box-sizing: border-box;")
        var textContent: String? = null
        var placeholderText: String? = null
        var linkUrl: String? = null
        var mediaSrc: String? = null
        var bgImageSrc: String? = null
        var videoSrc: String? = null
        var audioSrc: String? = null
        var iframeSrc: String? = null
        var fitVal: String? = null
        var altText: String? = null
        var posterSrc: String? = null
        var showControls: Boolean? = null
        var isAutoplay = false
        var isLoop = false
        var isMuted = false
        var zIndexVal: String? = null
        var positionVal: String? = null
        var allowVal: String? = null
        var allowFullscreen = true
        var tintColor: String? = null

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
                    when (constraint.direction) {
                        Direction.VERTICAL -> {
                            cssStyles.add("display: flex;")
                            cssStyles.add("flex-direction: column;")
                            if (constraint.gap > 0f) {
                                cssStyles.add("gap: ${constraint.gap.toIntIfWhole()}px;")
                            }
                            when (constraint.mainAxis) {
                                Sizing.FILL -> cssStyles.add("flex-grow: 1;")
                                Sizing.HUG -> cssStyles.add("height: fit-content;")
                                Sizing.FIXED -> {}
                            }
                            when (constraint.crossAxis) {
                                Sizing.FILL -> cssStyles.add("align-self: stretch;")
                                Sizing.HUG -> {}
                                Sizing.FIXED -> {}
                            }
                        }
                        Direction.HORIZONTAL -> {
                            cssStyles.add("display: flex;")
                            cssStyles.add("flex-direction: row;")
                            if (constraint.gap > 0f) {
                                cssStyles.add("gap: ${constraint.gap.toIntIfWhole()}px;")
                            }
                            when (constraint.mainAxis) {
                                Sizing.FILL -> cssStyles.add("flex-grow: 1;")
                                Sizing.HUG -> cssStyles.add("width: fit-content;")
                                Sizing.FIXED -> {}
                            }
                            when (constraint.crossAxis) {
                                Sizing.FILL -> cssStyles.add("align-self: stretch;")
                                Sizing.HUG -> {}
                                Sizing.FIXED -> {}
                            }
                        }
                        Direction.STACK -> {
                            cssStyles.add("position: relative;")
                            cssStyles.add("display: block;")
                        }
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
                            if (parentLayoutDir == "HORIZONTAL") {
                                cssStyles.add("flex: 1;")
                            }
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
                            if (parentLayoutDir == "VERTICAL") {
                                cssStyles.add("flex: 1;")
                            }
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
                "backgroundColor" -> {
                    cssStyles.add("background-color: $strVal;")
                }
                "alignItems" -> {
                    cssStyles.add("align-items: ${strVal.trim('"', '\'')};")
                }
                "justifyContent" -> {
                    cssStyles.add("justify-content: ${strVal.trim('"', '\'')};")
                }
                "placeholder" -> {
                    placeholderText = strVal
                }
                "text" -> {
                    textContent = strVal
                }
                "label" -> {
                    if (comp.children.isEmpty() && textContent == null) {
                        textContent = strVal
                    }
                }
                "href", "url", "link" -> {
                    linkUrl = strVal.trim('"', '\'')
                }
                "src", "image" -> {
                    mediaSrc = strVal
                }
                "backgroundImage" -> {
                    bgImageSrc = strVal
                }
                "video" -> {
                    videoSrc = strVal
                }
                "audio" -> {
                    audioSrc = strVal
                }
                "iframe" -> {
                    iframeSrc = strVal
                }
                "fit" -> {
                    fitVal = strVal.trim('"', '\'')
                }
                "alt" -> {
                    altText = strVal
                }
                "poster" -> {
                    posterSrc = strVal
                }
                "controls" -> {
                    showControls = strVal.toBoolean()
                }
                "autoplay" -> {
                    isAutoplay = strVal == "true"
                }
                "loop" -> {
                    isLoop = strVal == "true"
                }
                "muted" -> {
                    isMuted = strVal == "true"
                }
                "zIndex", "z" -> {
                    zIndexVal = strVal
                }
                "position" -> {
                    positionVal = strVal.trim('"', '\'')
                }
                "allow" -> {
                    allowVal = strVal.trim('"', '\'')
                }
                "allowFullscreen" -> {
                    allowFullscreen = strVal != "false"
                }
                "tint" -> {
                    tintColor = strVal
                }
            }
        }

        if (parentLayoutDir == "STACK") {
            val hasExplicitPos = comp.constraints.any { it is Constraint.Explicit && (it.axis == Axis.X || it.axis == Axis.Y) }
            if (!hasExplicitPos && positionVal == null) {
                cssStyles.add("position: absolute;")
                cssStyles.add("left: 0;")
                cssStyles.add("top: 0;")
            }
        }
        if (positionVal != null) {
            cssStyles.removeAll { it.startsWith("position:") }
            cssStyles.add("position: $positionVal;")
        }
        if (zIndexVal != null) {
            cssStyles.add("z-index: $zIndexVal;")
        }

        val effectiveBg = bgImageSrc ?: (if (comp.children.isNotEmpty()) mediaSrc else null)
        if (effectiveBg != null) {
            val resolvedBg = options.assetResolver(effectiveBg.trim('"', '\''))
            cssStyles.add("background-image: url('${escapeHtml(resolvedBg)}');")
            val bgSize = when (fitVal?.lowercase()) {
                "contain" -> "contain"
                "fill" -> "100% 100%"
                "none" -> "auto"
                else -> "cover"
            }
            cssStyles.add("background-size: $bgSize;")
            cssStyles.add("background-position: center;")
            cssStyles.add("background-repeat: no-repeat;")
        }

        if (placeholderText != null && comp.children.isEmpty()) {
            cssStyles.add("display: flex;")
            cssStyles.add("align-items: center;")
            if (comp.properties.none { it.key == "padding" }) {
                cssStyles.add("padding: 0 16px;")
            }
        } else if (comp.children.isNotEmpty() && comp.constraints.none { it is Constraint.AutoLayout }) {
            cssStyles.add("display: flex;")
            cssStyles.add("align-items: center;")
            cssStyles.add("justify-content: center;")
            cssStyles.add("cursor: pointer;")
        }

        if (linkUrl != null) {
            cssStyles.add("text-decoration: none;")
            cssStyles.add("cursor: pointer;")
            if (cssStyles.none { it.startsWith("display:") }) {
                cssStyles.add("display: inline-flex;")
            }
        }

        // Dedicated Media Element rendering for leaf components
        val isVideo = comp.children.isEmpty() && (videoSrc != null || mediaSrc?.endsWith(".mp4", ignoreCase = true) == true || mediaSrc?.endsWith(".webm", ignoreCase = true) == true)
        if (isVideo) {
            val vidUrl = options.assetResolver((videoSrc ?: mediaSrc!!).trim('"', '\''))
            val posterAttr = if (posterSrc != null) " poster=\"${escapeHtml(options.assetResolver(posterSrc!!.trim('"', '\'')))}\"" else ""
            val controlsAttr = if (showControls != false) " controls" else ""
            val autoplayAttr = if (isAutoplay) " autoplay" else ""
            val loopAttr = if (isLoop) " loop" else ""
            val mutedAttr = if (isMuted) " muted" else ""
            if (fitVal != null) cssStyles.add("object-fit: $fitVal;")
            val styleAttr = if (cssStyles.isNotEmpty()) " style=\"${cssStyles.joinToString(" ")}\"" else ""
            builder.append("$indent<video class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\" src=\"${escapeHtml(vidUrl)}\"$posterAttr$controlsAttr$autoplayAttr$loopAttr$mutedAttr$styleAttr></video>\n")
            return
        }

        val isAudio = comp.children.isEmpty() && (audioSrc != null || mediaSrc?.endsWith(".mp3", ignoreCase = true) == true || mediaSrc?.endsWith(".wav", ignoreCase = true) == true || mediaSrc?.endsWith(".ogg", ignoreCase = true) == true)
        if (isAudio) {
            val audUrl = options.assetResolver((audioSrc ?: mediaSrc!!).trim('"', '\''))
            val controlsAttr = if (showControls != false) " controls" else ""
            val autoplayAttr = if (isAutoplay) " autoplay" else ""
            val loopAttr = if (isLoop) " loop" else ""
            val mutedAttr = if (isMuted) " muted" else ""
            val styleAttr = if (cssStyles.isNotEmpty()) " style=\"${cssStyles.joinToString(" ")}\"" else ""
            builder.append("$indent<audio class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\" src=\"${escapeHtml(audUrl)}\"$controlsAttr$autoplayAttr$loopAttr$mutedAttr$styleAttr></audio>\n")
            return
        }

        val isIframe = comp.children.isEmpty() && (iframeSrc != null || (mediaSrc != null && (mediaSrc!!.contains("youtube.com") || mediaSrc!!.contains("youtu.be") || mediaSrc!!.contains("vimeo.com"))))
        if (isIframe) {
            val rawEmbedUrl = (iframeSrc ?: mediaSrc!!).trim('"', '\'')
            val embedUrl = toEmbedUrl(rawEmbedUrl)
            val fsAttr = if (allowFullscreen) " allowfullscreen" else ""
            val allowAttr = if (allowVal != null) " allow=\"${escapeHtml(allowVal)}\"" else " allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture\""
            cssStyles.add("border: none;")
            val styleAttr = if (cssStyles.isNotEmpty()) " style=\"${cssStyles.joinToString(" ")}\"" else ""
            builder.append("$indent<iframe class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\" src=\"${escapeHtml(embedUrl)}\" frameborder=\"0\"$allowAttr$fsAttr$styleAttr></iframe>\n")
            return
        }

        val isImage = comp.children.isEmpty() && mediaSrc != null && bgImageSrc == null
        if (isImage) {
            val imgUrl = options.assetResolver(mediaSrc!!.trim('"', '\''))
            val altAttr = " alt=\"${escapeHtml(altText ?: comp.name)}\""
            if (fitVal != null) {
                cssStyles.add("object-fit: $fitVal;")
            }
            if (tintColor != null && mediaSrc!!.endsWith(".svg", ignoreCase = true)) {
                cssStyles.add("color: $tintColor;")
            }
            val styleAttr = if (cssStyles.isNotEmpty()) " style=\"${cssStyles.joinToString(" ")}\"" else ""
            if (linkUrl != null) {
                builder.append("$indent<a class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\" href=\"${escapeHtml(linkUrl)}\" target=\"_blank\" rel=\"noopener noreferrer\"$styleAttr>\n")
                builder.append("$indent  <img src=\"${escapeHtml(imgUrl)}\"$altAttr style=\"width: 100%; height: 100%; object-fit: ${fitVal ?: "cover"}; border-radius: inherit; display: block;\" />\n")
                builder.append("$indent</a>\n")
            } else {
                builder.append("$indent<img class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\" src=\"${escapeHtml(imgUrl)}\"$altAttr$styleAttr />\n")
            }
            return
        }

        val styleAttr = if (cssStyles.isNotEmpty()) {
            " style=\"${cssStyles.joinToString(" ")}\""
        } else ""

        val tagName = if (linkUrl != null) "a" else "div"
        val hrefAttr = if (linkUrl != null) {
            " href=\"${escapeHtml(linkUrl)}\" target=\"_blank\" rel=\"noopener noreferrer\""
        } else ""

        builder.append("$indent<$tagName class=\"vireo-component\" data-name=\"${escapeHtml(comp.name)}\"$hrefAttr$styleAttr>")

        if (textContent != null) {
            builder.append(escapeHtml(textContent))
        } else if (placeholderText != null && comp.children.isEmpty()) {
            builder.append("<span style=\"color: #9CA3AF; font-size: 14px; user-select: none;\">${escapeHtml(placeholderText)}</span>")
        }

        val autoLayout = comp.constraints.filterIsInstance<Constraint.AutoLayout>().firstOrNull()
        val currentLayoutDir = when (autoLayout?.direction) {
            Direction.VERTICAL -> "VERTICAL"
            Direction.HORIZONTAL -> "HORIZONTAL"
            Direction.STACK -> "STACK"
            null -> null
        }

        if (comp.children.isNotEmpty()) {
            builder.append("\n")
            comp.children.forEach { child ->
                renderComponent(builder, child, "$indent  ", file, loadedFiles, options, currentLayoutDir)
            }
            builder.append(indent)
        }

        builder.append("</$tagName>\n")
    }

    private fun toEmbedUrl(url: String): String {
        val clean = url.trim('"', '\'')
        val ytWatch = Regex("""(?:https?:\/\/)?(?:www\.)?youtube\.com\/watch\?v=([a-zA-Z0-9_-]+)""").find(clean)
        if (ytWatch != null) {
            return "https://www.youtube.com/embed/${ytWatch.groupValues[1]}"
        }
        val ytShort = Regex("""(?:https?:\/\/)?youtu\.be\/([a-zA-Z0-9_-]+)""").find(clean)
        if (ytShort != null) {
            return "https://www.youtube.com/embed/${ytShort.groupValues[1]}"
        }
        return clean
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
