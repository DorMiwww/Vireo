package com.vireo.ideaplugin

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Colors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.vireo.lexer.TokenType as VTokenType

class DacSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = DacLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        val vType = DacTokenTypes.reverse(tokenType) ?: return TextAttributesKey.EMPTY_ARRAY
        val key = when (vType) {
            VTokenType.KEYWORD_BLOCK, VTokenType.KEYWORD_COMPONENT, VTokenType.KEYWORD_IMPORT,
            VTokenType.KEYWORD_FROM, VTokenType.KEYWORD_VAR, VTokenType.KEYWORD_FUN,
            VTokenType.KEYWORD_RETURN, VTokenType.KEYWORD_IF, VTokenType.KEYWORD_THEN,
            VTokenType.KEYWORD_ELSE, VTokenType.KEYWORD_REF -> KEYWORD

            VTokenType.STRING -> STRING
            VTokenType.NUMBER -> NUMBER
            VTokenType.HEX_COLOR -> HEX_COLOR
            VTokenType.IDENTIFIER -> IDENTIFIER

            VTokenType.COLON, VTokenType.DOT, VTokenType.COMMA, VTokenType.EQUALS,
            VTokenType.DOUBLE_EQUALS, VTokenType.DOLLAR, VTokenType.PERCENT,
            VTokenType.PLUS, VTokenType.MINUS, VTokenType.STAR, VTokenType.SLASH -> OPERATOR

            VTokenType.LBRACE, VTokenType.RBRACE -> BRACES
            VTokenType.LPAREN, VTokenType.RPAREN -> PARENS
            VTokenType.EOF -> return TextAttributesKey.EMPTY_ARRAY
        }
        return arrayOf(key)
    }

    companion object {
        val KEYWORD: TextAttributesKey = createTextAttributesKey("DAC_KEYWORD", Colors.KEYWORD)
        val STRING: TextAttributesKey = createTextAttributesKey("DAC_STRING", Colors.STRING)
        val NUMBER: TextAttributesKey = createTextAttributesKey("DAC_NUMBER", Colors.NUMBER)
        val HEX_COLOR: TextAttributesKey = createTextAttributesKey("DAC_HEX_COLOR", Colors.CONSTANT)
        val IDENTIFIER: TextAttributesKey = createTextAttributesKey("DAC_IDENTIFIER", Colors.IDENTIFIER)
        val OPERATOR: TextAttributesKey = createTextAttributesKey("DAC_OPERATOR", Colors.OPERATION_SIGN)
        val BRACES: TextAttributesKey = createTextAttributesKey("DAC_BRACES", Colors.BRACES)
        val PARENS: TextAttributesKey = createTextAttributesKey("DAC_PARENS", Colors.PARENTHESES)
    }
}

class DacSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) = DacSyntaxHighlighter()
}
