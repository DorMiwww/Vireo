package com.vireo.ideaplugin

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object DacFileType : LanguageFileType(DacLanguage) {
    override fun getName(): String = "Dac"
    override fun getDescription(): String = "Vireo .dac design-as-code source file"
    override fun getDefaultExtension(): String = "dac"
    override fun getIcon(): Icon = DacIcons.FILE
}
