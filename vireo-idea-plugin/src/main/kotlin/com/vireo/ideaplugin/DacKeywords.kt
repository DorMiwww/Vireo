package com.vireo.ideaplugin

/**
 * Hand-maintained completion vocabulary. Property.key is a plain String in the
 * AST (vireo-core), not a closed enum, so this list can't be derived from the
 * compiler — keep it in sync with SYNTAX.md / EXAMPLES.md / the renderers by hand.
 */
object DacKeywords {
    // Valid at file scope: before/between block declarations.
    val TOP_LEVEL = listOf("block", "import", "from", "var", "fun")

    // Valid inside a block or component body, alongside property keys.
    val NESTED = listOf("component", "ref", "if", "then", "else", "return")

    // Known property keys, gathered from vireo-renderer-html / vireo-renderer-figma
    // / vireo-analysis's numeric-property validation.
    val PROPERTY_KEYS = listOf(
        "width", "height", "x", "y",
        "color", "backgroundColor", "text", "label", "placeholder",
        "fontSize", "fontWeight", "radius", "padding", "border", "shadow",
        "layout", "mainAxis", "crossAxis", "gap",
        "alignItems", "justifyContent"
    )
}
