package com.vireo.ideaplugin

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/**
 * Context is approximated by brace depth up to the caret (count of '{' minus
 * '}' in the text before it), not a real PSI scope walk — cheap and correct
 * enough for "am I inside a block/component body" without needing a full
 * grammar. See ROADMAP.md Phase 7 for why this is the deliberate v1 shape.
 */
class DacCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val offset = parameters.offset
                    val textBeforeCaret = parameters.editor.document.charsSequence
                        .subSequence(0, offset.coerceIn(0, parameters.editor.document.textLength))
                    val depth = textBeforeCaret.count { it == '{' } - textBeforeCaret.count { it == '}' }

                    val suggestions = if (depth <= 0) {
                        DacKeywords.TOP_LEVEL
                    } else {
                        DacKeywords.NESTED + DacKeywords.PROPERTY_KEYS
                    }

                    for (word in suggestions) {
                        result.addElement(LookupElementBuilder.create(word))
                    }
                }
            }
        )
    }
}
