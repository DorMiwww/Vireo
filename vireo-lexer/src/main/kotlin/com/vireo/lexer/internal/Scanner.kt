package com.vireo.lexer

import com.vireo.core.SourceLocation
import com.vireo.core.VireoError
import com.vireo.core.VireoResult

private val KEYWORDS = mapOf(
    "block" to TokenType.KEYWORD_BLOCK,
    "component" to TokenType.KEYWORD_COMPONENT,
    "import" to TokenType.KEYWORD_IMPORT,
    "from" to TokenType.KEYWORD_FROM,
    "var" to TokenType.KEYWORD_VAR,
    "fun" to TokenType.KEYWORD_FUN,
    "return" to TokenType.KEYWORD_RETURN,
    "if" to TokenType.KEYWORD_IF,
    "then" to TokenType.KEYWORD_THEN,
    "else" to TokenType.KEYWORD_ELSE,
    "ref" to TokenType.KEYWORD_REF
)

internal class Scanner(
    private val source: String,
    private val filePath: String
) {
    private val tokens = mutableListOf<Token>()
    private val errors = mutableListOf<VireoError>()

    private var index = 0
    private var line = 1
    private var column = 1

    fun scanAll(): VireoResult<List<Token>> {
        while (!isAtEnd()) {
            val startLine = line
            val startColumn = column
            val ch = advance()

            when {
                ch == ' ' || ch == '\t' || ch == '\r' -> {
                    // Whitespace, ignore
                }
                ch == '\n' -> {
                    // Handled in advance()
                }
                ch == '/' -> {
                    if (peek() == '/') {
                        // Single line comment
                        advance()
                        while (!isAtEnd() && peek() != '\n') {
                            advance()
                        }
                    } else if (peek() == '*') {
                        // Multi-line comment
                        advance()
                        val commentStartLoc = SourceLocation(filePath, startLine, startColumn)
                        var closed = false
                        while (!isAtEnd()) {
                            if (peek() == '*' && peekNext() == '/') {
                                advance() // consume '*'
                                advance() // consume '/'
                                closed = true
                                break
                            }
                            advance()
                        }
                        if (!closed) {
                            errors.add(
                                VireoError(
                                    "Unterminated multi-line comment",
                                    commentStartLoc
                                )
                            )
                        }
                    } else {
                        tokens.add(Token(TokenType.SLASH, "/", SourceLocation(filePath, startLine, startColumn)))
                    }
                }
                ch == ':' -> tokens.add(Token(TokenType.COLON, ":", SourceLocation(filePath, startLine, startColumn)))
                ch == '.' -> tokens.add(Token(TokenType.DOT, ".", SourceLocation(filePath, startLine, startColumn)))
                ch == ',' -> tokens.add(Token(TokenType.COMMA, ",", SourceLocation(filePath, startLine, startColumn)))
                ch == '=' -> {
                    if (peek() == '=') {
                        advance()
                        tokens.add(Token(TokenType.DOUBLE_EQUALS, "==", SourceLocation(filePath, startLine, startColumn)))
                    } else {
                        tokens.add(Token(TokenType.EQUALS, "=", SourceLocation(filePath, startLine, startColumn)))
                    }
                }
                ch == '$' -> tokens.add(Token(TokenType.DOLLAR, "$", SourceLocation(filePath, startLine, startColumn)))
                ch == '%' -> tokens.add(Token(TokenType.PERCENT, "%", SourceLocation(filePath, startLine, startColumn)))
                ch == '+' -> tokens.add(Token(TokenType.PLUS, "+", SourceLocation(filePath, startLine, startColumn)))
                ch == '-' -> tokens.add(Token(TokenType.MINUS, "-", SourceLocation(filePath, startLine, startColumn)))
                ch == '*' -> tokens.add(Token(TokenType.STAR, "*", SourceLocation(filePath, startLine, startColumn)))
                ch == '{' -> tokens.add(Token(TokenType.LBRACE, "{", SourceLocation(filePath, startLine, startColumn)))
                ch == '}' -> tokens.add(Token(TokenType.RBRACE, "}", SourceLocation(filePath, startLine, startColumn)))
                ch == '(' -> tokens.add(Token(TokenType.LPAREN, "(", SourceLocation(filePath, startLine, startColumn)))
                ch == ')' -> tokens.add(Token(TokenType.RPAREN, ")", SourceLocation(filePath, startLine, startColumn)))
                ch == '"' -> scanString(startLine, startColumn)
                ch == '#' -> scanHexColor(startLine, startColumn)
                ch.isDigit() -> scanNumber(ch, startLine, startColumn)
                ch.isIdentifierStart() -> scanIdentifierOrKeyword(ch, startLine, startColumn)
                else -> {
                    errors.add(
                        VireoError(
                            "Unexpected character '$ch'",
                            SourceLocation(filePath, startLine, startColumn)
                        )
                    )
                }
            }
        }

        tokens.add(Token(TokenType.EOF, "", SourceLocation(filePath, line, column)))

        return if (errors.isNotEmpty()) {
            VireoResult.Err(errors)
        } else {
            VireoResult.Ok(tokens)
        }
    }

    private fun scanString(startLine: Int, startColumn: Int) {
        val sb = StringBuilder()
        val startLoc = SourceLocation(filePath, startLine, startColumn)

        while (!isAtEnd()) {
            val c = peek()
            if (c == '"') {
                advance() // consume closing quote
                tokens.add(Token(TokenType.STRING, sb.toString(), startLoc))
                return
            }
            if (c == '\n') {
                // String literal cannot span raw newline without escape
                break
            }
            if (c == '\\') {
                advance() // consume '\'
                if (isAtEnd()) break
                when (val esc = advance()) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    else -> sb.append(esc)
                }
            } else {
                sb.append(advance())
            }
        }

        errors.add(VireoError("Unterminated string literal", startLoc))
    }

    private fun scanHexColor(startLine: Int, startColumn: Int) {
        val sb = StringBuilder("#")
        val startLoc = SourceLocation(filePath, startLine, startColumn)

        while (!isAtEnd() && peek().isHexDigit()) {
            sb.append(advance())
        }

        val colorStr = sb.toString()
        val hexDigits = colorStr.substring(1)
        if (hexDigits.length in listOf(3, 4, 6, 8)) {
            tokens.add(Token(TokenType.HEX_COLOR, colorStr, startLoc))
        } else {
            errors.add(VireoError("Invalid hex color format '$colorStr'", startLoc))
        }
    }

    private fun scanNumber(firstChar: Char, startLine: Int, startColumn: Int) {
        val sb = StringBuilder().append(firstChar)
        val startLoc = SourceLocation(filePath, startLine, startColumn)

        while (!isAtEnd() && peek().isDigit()) {
            sb.append(advance())
        }

        if (!isAtEnd() && peek() == '.' && peekNext().isDigit()) {
            sb.append(advance()) // consume '.'
            while (!isAtEnd() && peek().isDigit()) {
                sb.append(advance())
            }
        }

        tokens.add(Token(TokenType.NUMBER, sb.toString(), startLoc))
    }

    private fun scanIdentifierOrKeyword(firstChar: Char, startLine: Int, startColumn: Int) {
        val sb = StringBuilder().append(firstChar)
        val startLoc = SourceLocation(filePath, startLine, startColumn)

        while (!isAtEnd() && peek().isIdentifierPart()) {
            sb.append(advance())
        }

        val text = sb.toString()
        val type = KEYWORDS[text] ?: TokenType.IDENTIFIER
        tokens.add(Token(type, text, startLoc))
    }

    private fun advance(): Char {
        val ch = source[index]
        index++
        if (ch == '\n') {
            line++
            column = 1
        } else {
            column++
        }
        return ch
    }

    private fun peek(): Char = if (isAtEnd()) ' ' else source[index]
    private fun peekNext(): Char = if (index + 1 >= source.length) ' ' else source[index + 1]

    private fun isAtEnd(): Boolean = index >= source.length

    private fun Char.isIdentifierStart(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
    private fun Char.isIdentifierPart(): Boolean = isIdentifierStart() || this in '0'..'9'
    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
