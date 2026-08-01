package com.vireo.lexer

import com.vireo.core.SourceLocation

data class Token(
    val type: TokenType,
    val value: String,
    val location: SourceLocation
)

enum class TokenType {
    // Keywords
    KEYWORD_BLOCK,       // block
    KEYWORD_COMPONENT,   // component
    KEYWORD_IMPORT,      // import
    KEYWORD_FROM,        // from
    KEYWORD_VAR,         // var
    KEYWORD_FUN,         // fun
    KEYWORD_RETURN,      // return
    KEYWORD_IF,          // if
    KEYWORD_THEN,        // then
    KEYWORD_ELSE,        // else
    KEYWORD_REF,         // ref

    // Literals & Identifiers
    IDENTIFIER,          // e.g. Main, Box, width, fontSize, bold
    NUMBER,              // e.g. 200, 100, 14, 3.14
    STRING,              // e.g. "Button", "./buttons.dac"
    HEX_COLOR,           // e.g. #3B82F6, #FFFFFF

    // Symbols & Punctuation
    COLON,               // :
    DOT,                 // .
    COMMA,               // ,
    EQUALS,              // =
    DOUBLE_EQUALS,       // ==
    DOLLAR,              // $
    PERCENT,             // %
    PLUS,                // +
    MINUS,               // -
    STAR,                // *
    SLASH,               // /

    // Delimiters
    LBRACE,              // {
    RBRACE,              // }
    LPAREN,              // (
    RPAREN,              // )

    // Special
    EOF                  // End of file
}
