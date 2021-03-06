// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.formatter

import com.intellij.lang.html.HTMLLanguage
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions
import com.intellij.psi.codeStyle.FileIndentOptionsProvider
import com.intellij.psi.formatter.xml.HtmlCodeStyleSettings
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxFileIndentOptionsProvider : FileIndentOptionsProvider() {

  override fun getIndentOptions(settings: CodeStyleSettings, file: PsiFile): IndentOptions? {
    if (file.language is MpxLanguage) {
      val fileType = file.originalFile.virtualFile?.fileType
      if (fileType === null || fileType === MpxFileType.INSTANCE) {
        return if (settings.getCustomSettings(MpxCodeStyleSettings::class.java).UNIFORM_INDENT)
          settings.getLanguageIndentOptions(MpxLanguage.INSTANCE)
            ?.clone()
            ?.castSafelyTo<IndentOptions>()
            ?.also { it.isOverrideLanguageOptions = true }
        else
          settings.getLanguageIndentOptions(HTMLLanguage.INSTANCE)
      }
      // Treat file as an HTML file and apply HTML indentation settings
      var htmlIndentOptions = settings.getLanguageIndentOptions(HTMLLanguage.INSTANCE)
      if (settings.getCustomSettings(HtmlCodeStyleSettings::class.java).HTML_UNIFORM_INDENT) {
        htmlIndentOptions = htmlIndentOptions.clone() as IndentOptions
        htmlIndentOptions.isOverrideLanguageOptions = true
      }
      return htmlIndentOptions
    }
    return null
  }
}