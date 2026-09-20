package com.vireo.ideaplugin

import com.intellij.psi.tree.IElementType
import com.vireo.lexer.TokenType as VTokenType

class DacElementType(debugName: String) : IElementType(debugName, DacLanguage)

/**
 * Bridges com.vireo.lexer.TokenType (vireo-lexer's batch token enum) to IntelliJ's
 * IElementType, which the platform's Lexer/SyntaxHighlighter/PSI machinery expects.
 */
object DacTokenTypes {
    private val toIj: Map<VTokenType, IElementType> =
        VTokenType.entries.associateWith { DacElementType(it.name) }
    private val toV: Map<IElementType, VTokenType> =
        toIj.entries.associate { (k, v) -> v to k }

    fun of(type: VTokenType): IElementType = toIj.getValue(type)
    fun reverse(type: IElementType): VTokenType? = toV[type]

    // Used when vireo-lexer fails to tokenize the buffer at all (rare — e.g. an
    // unterminated string/comment); keeps the editor from crashing on invalid input.
    val BAD_CHARACTER: IElementType = DacElementType("DAC_BAD_CHARACTER")
}
