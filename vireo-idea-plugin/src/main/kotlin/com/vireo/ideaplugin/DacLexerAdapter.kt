package com.vireo.ideaplugin

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType as IjTokenType
import com.intellij.psi.tree.IElementType
import com.vireo.core.VireoResult
import com.vireo.lexer.Lexer
import com.vireo.lexer.Token
import com.vireo.lexer.TokenType as VTokenType

/**
 * Wraps vireo-lexer's whole-buffer Lexer.tokenize() behind IntelliJ's offset-based
 * Lexer contract. Not a true incremental lexer — it re-tokenizes the full text on
 * every start() call — which is fine at .dac file sizes (small UI descriptions),
 * same tradeoff every simple custom-language plugin makes over a batch lexer.
 *
 * vireo-lexer's Token only carries a line/column SourceLocation, not a character
 * offset, and STRING token values have quotes stripped and escapes resolved (so
 * they no longer match the raw source substring). To recover exact offsets:
 *  - every non-STRING token's `value` is an exact copy of its source text, so its
 *    length gives the end offset directly;
 *  - STRING token end offsets are found by a small local re-scan from the opening
 *    quote, mirroring vireo-lexer's own escape handling.
 * Gaps between tokens (whitespace, and comments, which vireo-lexer silently
 * consumes) are emitted as synthetic WHITE_SPACE pieces so every character in the
 * buffer is covered, as IntelliJ's Lexer contract requires.
 */
class DacLexerAdapter : LexerBase() {

    private data class Piece(val type: IElementType, val start: Int, val end: Int)

    private var bufferSeq: CharSequence = ""
    private var bufferEndOffset: Int = 0
    private var pieces: List<Piece> = emptyList()
    private var index: Int = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.bufferSeq = buffer
        this.bufferEndOffset = endOffset
        this.pieces = buildPieces(buffer, startOffset, endOffset)
        this.index = 0
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = pieces.getOrNull(index)?.type

    override fun getTokenStart(): Int = pieces.getOrNull(index)?.start ?: bufferEndOffset

    override fun getTokenEnd(): Int = pieces.getOrNull(index)?.end ?: bufferEndOffset

    override fun advance() {
        index++
    }

    override fun getBufferSequence(): CharSequence = bufferSeq

    override fun getBufferEnd(): Int = bufferEndOffset

    private fun buildPieces(buffer: CharSequence, startOffset: Int, endOffset: Int): List<Piece> {
        val text = buffer.subSequence(startOffset, endOffset).toString()
        if (text.isEmpty()) return emptyList()

        val tokens: List<Token> = when (val result = Lexer.tokenize(text)) {
            is VireoResult.Ok -> result.value
            is VireoResult.Err -> return listOf(Piece(DacTokenTypes.BAD_CHARACTER, startOffset, endOffset))
        }

        val lineStarts = computeLineStarts(text)
        val pieces = mutableListOf<Piece>()
        var cursor = 0

        for (token in tokens) {
            if (token.type == VTokenType.EOF) break

            val start = offsetOf(lineStarts, token.location.line, token.location.column)
                .coerceIn(cursor, text.length)

            if (start > cursor) {
                pieces.add(Piece(IjTokenType.WHITE_SPACE, startOffset + cursor, startOffset + start))
            }

            val rawEnd = if (token.type == VTokenType.STRING) {
                findStringTokenEnd(text, start)
            } else {
                start + token.value.length
            }
            val end = rawEnd.coerceIn(start, text.length)

            pieces.add(Piece(DacTokenTypes.of(token.type), startOffset + start, startOffset + end))
            cursor = end
        }

        if (cursor < text.length) {
            pieces.add(Piece(IjTokenType.WHITE_SPACE, startOffset + cursor, endOffset))
        }

        return pieces
    }

    private fun offsetOf(lineStarts: IntArray, line: Int, column: Int): Int {
        val lineIndex = (line - 1).coerceIn(0, lineStarts.lastIndex)
        return lineStarts[lineIndex] + (column - 1)
    }

    private fun findStringTokenEnd(text: String, quoteStart: Int): Int {
        var i = quoteStart + 1
        while (i < text.length) {
            when (text[i]) {
                '\\' -> i += 2
                '"' -> return i + 1
                else -> i += 1
            }
        }
        return text.length
    }

    private fun computeLineStarts(text: CharSequence): IntArray {
        val starts = ArrayList<Int>(text.length / 32 + 2)
        starts.add(0)
        for (i in text.indices) {
            if (text[i] == '\n') starts.add(i + 1)
        }
        return starts.toIntArray()
    }
}
