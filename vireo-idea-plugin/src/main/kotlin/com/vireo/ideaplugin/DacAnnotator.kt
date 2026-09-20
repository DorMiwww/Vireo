package com.vireo.ideaplugin

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.vireo.analysis.Analyzer
import com.vireo.core.VireoError
import com.vireo.core.VireoResult
import com.vireo.parser.Parser

data class DacAnnotationInput(val text: String, val filePath: String)

/**
 * Runs the real vireo-parser -> vireo-analysis pipeline in-process (no CLI
 * subprocess) against the editor's current text and reports every VireoError at
 * its SourceLocation. ExternalAnnotator.doAnnotate() runs off the EDT, which
 * matches vireo-analysis being a plain, side-effect-free (besides reading
 * imported .dac files from disk) Kotlin library — safe to call directly here.
 */
class DacAnnotator : ExternalAnnotator<DacAnnotationInput, List<VireoError>>() {

    override fun collectInformation(file: PsiFile): DacAnnotationInput? {
        val path = file.virtualFile?.path ?: return null
        return DacAnnotationInput(file.text, path)
    }

    override fun doAnnotate(input: DacAnnotationInput): List<VireoError> {
        return when (val parseResult = Parser.parse(input.text, input.filePath)) {
            is VireoResult.Err -> parseResult.errors
            is VireoResult.Ok -> when (val analyzeResult = Analyzer.analyze(parseResult.value)) {
                is VireoResult.Err -> analyzeResult.errors
                is VireoResult.Ok -> emptyList()
            }
        }
    }

    override fun apply(file: PsiFile, errors: List<VireoError>, holder: AnnotationHolder) {
        if (errors.isEmpty()) return
        val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return

        for (error in errors) {
            val lineIndex = (error.location.line - 1).coerceIn(0, maxOf(document.lineCount - 1, 0))
            val lineStart = document.getLineStartOffset(lineIndex)
            val start = (lineStart + (error.location.column - 1).coerceAtLeast(0))
                .coerceIn(lineStart, document.textLength)
            val end = (start + 1).coerceIn(start, document.textLength)

            holder.newAnnotation(HighlightSeverity.ERROR, error.message)
                .range(com.intellij.openapi.util.TextRange(start, end))
                .create()
        }
    }
}
