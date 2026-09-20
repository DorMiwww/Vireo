package com.vireo.ideaplugin

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Deliberately minimal: builds a flat PSI tree (every token as a direct leaf
 * child of the file), no nested grammar rules. That's enough to back a
 * SyntaxHighlighter, an ExternalAnnotator and a CompletionContributor. Full
 * structural PSI (needed for go-to-definition on file.block.component refs) is
 * explicitly out of scope for this phase — see ROADMAP.md, Phase 7.
 */
class DacParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = DacLexerAdapter()

    override fun createParser(project: Project): PsiParser = PsiParser { root, builder ->
        val rootMarker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        rootMarker.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet =
        TokenSet.create(DacTokenTypes.of(com.vireo.lexer.TokenType.STRING))

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = DacFile(viewProvider)

    companion object {
        val FILE = IFileElementType(DacLanguage)
    }
}

class DacFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DacLanguage) {
    override fun getFileType(): FileType = DacFileType
}
