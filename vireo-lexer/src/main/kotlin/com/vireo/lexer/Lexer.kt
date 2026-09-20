package com.vireo.lexer

import com.vireo.core.VireoResult

object Lexer {
    fun tokenize(source: String, filePath: String = "<anonymous>"): VireoResult<List<Token>> {
        val scanner = Scanner(source, filePath)
        return scanner.scanAll()
    }
}
