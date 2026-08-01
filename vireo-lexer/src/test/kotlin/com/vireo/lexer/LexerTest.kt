package com.vireo.lexer

import com.vireo.core.SourceLocation
import com.vireo.core.VireoResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LexerTest {

    private data class ExpectedToken(
        val type: TokenType,
        val value: String
    )

    @Test
    fun `test every token in Example 1 - Hello Rectangle is produced in correct order with correct type and value`() {
        val source = """
            block Main {
                component Box {
                    width: 200
                    height: 100
                    color: #3B82F6
                    radius: 8
                }
            }
        """.trimIndent()

        val filePath = "example1.dac"
        val result = Lexer.tokenize(source, filePath)
        assertTrue(result is VireoResult.Ok, "Expected VireoResult.Ok but got $result")

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.KEYWORD_BLOCK, "block"),
            ExpectedToken(TokenType.IDENTIFIER, "Main"),
            ExpectedToken(TokenType.LBRACE, "{"),
            ExpectedToken(TokenType.KEYWORD_COMPONENT, "component"),
            ExpectedToken(TokenType.IDENTIFIER, "Box"),
            ExpectedToken(TokenType.LBRACE, "{"),
            ExpectedToken(TokenType.IDENTIFIER, "width"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "200"),
            ExpectedToken(TokenType.IDENTIFIER, "height"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "100"),
            ExpectedToken(TokenType.IDENTIFIER, "color"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.HEX_COLOR, "#3B82F6"),
            ExpectedToken(TokenType.IDENTIFIER, "radius"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "8"),
            ExpectedToken(TokenType.RBRACE, "}"),
            ExpectedToken(TokenType.RBRACE, "}"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size, "Token count mismatch for Example 1")
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Token type mismatch at index $i")
            assertEquals(expected[i].value, tokens[i].value, "Token value mismatch at index $i")
        }
    }

    @Test
    fun `test every token in Example 2 - Button Component is produced correctly`() {
        val source = """
            block Primary {
                component Default {
                    width: 120
                    height: 40
                    color: #3B82F6
                    radius: 8

                    component Label {
                        text: "Button"
                        fontSize: 14
                        fontWeight: bold
                        color: #FFFFFF
                    }
                }
            }
        """.trimIndent()

        val filePath = "buttons.dac"
        val result = Lexer.tokenize(source, filePath)
        assertTrue(result is VireoResult.Ok, "Expected VireoResult.Ok but got $result")

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.KEYWORD_BLOCK, "block"),
            ExpectedToken(TokenType.IDENTIFIER, "Primary"),
            ExpectedToken(TokenType.LBRACE, "{"),
            ExpectedToken(TokenType.KEYWORD_COMPONENT, "component"),
            ExpectedToken(TokenType.IDENTIFIER, "Default"),
            ExpectedToken(TokenType.LBRACE, "{"),
            ExpectedToken(TokenType.IDENTIFIER, "width"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "120"),
            ExpectedToken(TokenType.IDENTIFIER, "height"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "40"),
            ExpectedToken(TokenType.IDENTIFIER, "color"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.HEX_COLOR, "#3B82F6"),
            ExpectedToken(TokenType.IDENTIFIER, "radius"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "8"),
            ExpectedToken(TokenType.KEYWORD_COMPONENT, "component"),
            ExpectedToken(TokenType.IDENTIFIER, "Label"),
            ExpectedToken(TokenType.LBRACE, "{"),
            ExpectedToken(TokenType.IDENTIFIER, "text"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.STRING, "Button"),
            ExpectedToken(TokenType.IDENTIFIER, "fontSize"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.NUMBER, "14"),
            ExpectedToken(TokenType.IDENTIFIER, "fontWeight"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.IDENTIFIER, "bold"),
            ExpectedToken(TokenType.IDENTIFIER, "color"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.HEX_COLOR, "#FFFFFF"),
            ExpectedToken(TokenType.RBRACE, "}"),
            ExpectedToken(TokenType.RBRACE, "}"),
            ExpectedToken(TokenType.RBRACE, "}"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size, "Token count mismatch for Example 2")
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Token type mismatch at index $i: expected ${expected[i]} but got ${tokens[i]}")
            assertEquals(expected[i].value, tokens[i].value, "Token value mismatch at index $i: expected ${expected[i]} but got ${tokens[i]}")
        }
    }

    @Test
    fun `test error case unterminated string literal returns VireoResult Err with a SourceLocation`() {
        val source = """
            component Box {
                text: "unclosed string literal
            }
        """.trimIndent()
        val filePath = "test_unclosed.dac"
        val result = Lexer.tokenize(source, filePath)

        assertTrue(result is VireoResult.Err, "Expected VireoResult.Err for unterminated string literal")
        assertEquals(1, result.errors.size, "Expected exactly 1 error")

        val error = result.errors[0]
        assertEquals("Unterminated string literal", error.message)
        assertEquals(SourceLocation(filePath, 2, 11), error.location)
    }

    @Test
    fun `test SourceLocation line and column are correct for non-trivial tokens`() {
        val source = """
            block Primary {
                component Default {
                    width: 120
                    height: 40
                    color: #3B82F6
                    radius: 8

                    component Label {
                        text: "Button"
                        fontSize: 14
                        fontWeight: bold
                        color: #FFFFFF
                    }
                }
            }
        """.trimIndent()

        val filePath = "buttons.dac"
        val result = Lexer.tokenize(source, filePath)
        assertTrue(result is VireoResult.Ok, "Expected VireoResult.Ok")

        val tokens = result.value

        // Check non-trivial token 1: #3B82F6 on line 5, col 16
        val colorHexToken = tokens[14]
        assertEquals(TokenType.HEX_COLOR, colorHexToken.type)
        assertEquals("#3B82F6", colorHexToken.value)
        assertEquals(SourceLocation(filePath, 5, 16), colorHexToken.location)

        // Check non-trivial token 2: STRING "Button" on line 9, col 19
        val stringToken = tokens[23]
        assertEquals(TokenType.STRING, stringToken.type)
        assertEquals("Button", stringToken.value)
        assertEquals(SourceLocation(filePath, 9, 19), stringToken.location)

        // Check non-trivial token 3: IDENTIFIER "fontWeight" on line 11, col 13
        val fontWeightToken = tokens[27]
        assertEquals(TokenType.IDENTIFIER, fontWeightToken.type)
        assertEquals("fontWeight", fontWeightToken.value)
        assertEquals(SourceLocation(filePath, 11, 13), fontWeightToken.location)

        // Check non-trivial token 4: HEX_COLOR "#FFFFFF" on line 12, col 20
        val whiteHexToken = tokens[32]
        assertEquals(TokenType.HEX_COLOR, whiteHexToken.type)
        assertEquals("#FFFFFF", whiteHexToken.value)
        assertEquals(SourceLocation(filePath, 12, 20), whiteHexToken.location)
    }

    @Test
    fun `lexer is stateless - same input produces identical tokens`() {
        val source = "block A { component B { width: 10 } }"
        val result1 = Lexer.tokenize(source, "test.dac")
        val result2 = Lexer.tokenize(source, "test.dac")

        assertEquals(result1, result2)
    }

    @Test
    fun `returns VireoResult Err for invalid hex color without throwing`() {
        val source = "color: #3B82FG"
        val result = Lexer.tokenize(source, "test.dac")

        assertTrue(result is VireoResult.Err, "Expected VireoResult.Err")
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].message.contains("Invalid hex color format"))
        assertEquals(SourceLocation("test.dac", 1, 8), result.errors[0].location)
    }

    @Test
    fun `returns VireoResult Err for unexpected characters without throwing`() {
        val source = "width: 100 @ height"
        val result = Lexer.tokenize(source, "test.dac")

        assertTrue(result is VireoResult.Err, "Expected VireoResult.Err")
        assertEquals(1, result.errors.size)
        assertEquals("Unexpected character '@'", result.errors[0].message)
        assertEquals(SourceLocation("test.dac", 1, 12), result.errors[0].location)
    }

    @Test
    fun `handles single line and multi line comments correctly`() {
        val source = """
            // Single line comment
            block Main { /* inline comment */ }
        """.trimIndent()

        val result = Lexer.tokenize(source, "test.dac")
        assertTrue(result is VireoResult.Ok)

        val types = result.value.map { it.type }
        assertEquals(
            listOf(
                TokenType.KEYWORD_BLOCK,
                TokenType.IDENTIFIER,
                TokenType.LBRACE,
                TokenType.RBRACE,
                TokenType.EOF
            ),
            types
        )
    }

    @Test
    fun `tokenizes imports and variables`() {
        val source = """
            import buttons from "./buttons.dac"
            var primaryColor = #3B82F6
        """.trimIndent()

        val result = Lexer.tokenize(source, "test.dac")
        assertTrue(result is VireoResult.Ok)

        val types = result.value.map { it.type }
        assertEquals(
            listOf(
                TokenType.KEYWORD_IMPORT, TokenType.IDENTIFIER, TokenType.KEYWORD_FROM, TokenType.STRING,
                TokenType.KEYWORD_VAR, TokenType.IDENTIFIER, TokenType.EQUALS, TokenType.HEX_COLOR,
                TokenType.EOF
            ),
            types
        )
    }

    @Test
    fun `Task 2-A - tokenizes import keyword and relative file paths`() {
        val source = """
            import buttons from "./buttons.dac"
            import forms from "../forms/login.dac"
        """.trimIndent()

        val result = Lexer.tokenize(source, "imports.dac")
        assertTrue(result is VireoResult.Ok, "Expected VireoResult.Ok")

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.KEYWORD_IMPORT, "import"),
            ExpectedToken(TokenType.IDENTIFIER, "buttons"),
            ExpectedToken(TokenType.KEYWORD_FROM, "from"),
            ExpectedToken(TokenType.STRING, "./buttons.dac"),
            ExpectedToken(TokenType.KEYWORD_IMPORT, "import"),
            ExpectedToken(TokenType.IDENTIFIER, "forms"),
            ExpectedToken(TokenType.KEYWORD_FROM, "from"),
            ExpectedToken(TokenType.STRING, "../forms/login.dac"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size)
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Mismatch at token index $i")
            assertEquals(expected[i].value, tokens[i].value, "Mismatch at token index $i")
        }
    }

    @Test
    fun `Task 2-A - tokenizes dot notation tokens for cross file references`() {
        val source = "ref: buttons.Primary.Default"

        val result = Lexer.tokenize(source, "ref.dac")
        assertTrue(result is VireoResult.Ok, "Expected VireoResult.Ok")

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.KEYWORD_REF, "ref"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.IDENTIFIER, "buttons"),
            ExpectedToken(TokenType.DOT, "."),
            ExpectedToken(TokenType.IDENTIFIER, "Primary"),
            ExpectedToken(TokenType.DOT, "."),
            ExpectedToken(TokenType.IDENTIFIER, "Default"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size)
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Mismatch at token index $i")
            assertEquals(expected[i].value, tokens[i].value, "Mismatch at token index $i")
        }
    }

    @Test
    fun `Task 3-A - tokenizes var declarations`() {
        val source = "var primaryColor = #3B82F6"
        val result = Lexer.tokenize(source, "var.dac")
        assertTrue(result is VireoResult.Ok)

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.KEYWORD_VAR, "var"),
            ExpectedToken(TokenType.IDENTIFIER, "primaryColor"),
            ExpectedToken(TokenType.EQUALS, "="),
            ExpectedToken(TokenType.HEX_COLOR, "#3B82F6"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size)
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Token type mismatch at index $i")
            assertEquals(expected[i].value, tokens[i].value, "Token value mismatch at index $i")
        }
    }

    @Test
    fun `Task 3-A - tokenizes fun declarations`() {
        val source = "fun spacing(multiplier: Int): Int { return 8 * multiplier }"
        val result = Lexer.tokenize(source, "fun.dac")
        assertTrue(result is VireoResult.Ok)

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.KEYWORD_FUN, "fun"),
            ExpectedToken(TokenType.IDENTIFIER, "spacing"),
            ExpectedToken(TokenType.LPAREN, "("),
            ExpectedToken(TokenType.IDENTIFIER, "multiplier"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.IDENTIFIER, "Int"),
            ExpectedToken(TokenType.RPAREN, ")"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.IDENTIFIER, "Int"),
            ExpectedToken(TokenType.LBRACE, "{"),
            ExpectedToken(TokenType.KEYWORD_RETURN, "return"),
            ExpectedToken(TokenType.NUMBER, "8"),
            ExpectedToken(TokenType.STAR, "*"),
            ExpectedToken(TokenType.IDENTIFIER, "multiplier"),
            ExpectedToken(TokenType.RBRACE, "}"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size)
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Token type mismatch at index $i")
            assertEquals(expected[i].value, tokens[i].value, "Token value mismatch at index $i")
        }
    }

    @Test
    fun `Task 3-A - tokenizes conditional expressions with double equals and dollar references`() {
        val source = "color: if \$variant == \"primary\" then #3B82F6 else #6B7280"
        val result = Lexer.tokenize(source, "cond.dac")
        assertTrue(result is VireoResult.Ok)

        val tokens = result.value
        val expected = listOf(
            ExpectedToken(TokenType.IDENTIFIER, "color"),
            ExpectedToken(TokenType.COLON, ":"),
            ExpectedToken(TokenType.KEYWORD_IF, "if"),
            ExpectedToken(TokenType.DOLLAR, "$"),
            ExpectedToken(TokenType.IDENTIFIER, "variant"),
            ExpectedToken(TokenType.DOUBLE_EQUALS, "=="),
            ExpectedToken(TokenType.STRING, "primary"),
            ExpectedToken(TokenType.KEYWORD_THEN, "then"),
            ExpectedToken(TokenType.HEX_COLOR, "#3B82F6"),
            ExpectedToken(TokenType.KEYWORD_ELSE, "else"),
            ExpectedToken(TokenType.HEX_COLOR, "#6B7280"),
            ExpectedToken(TokenType.EOF, "")
        )

        assertEquals(expected.size, tokens.size)
        for (i in tokens.indices) {
            assertEquals(expected[i].type, tokens[i].type, "Token type mismatch at index $i")
            assertEquals(expected[i].value, tokens[i].value, "Token value mismatch at index $i")
        }
    }
}


