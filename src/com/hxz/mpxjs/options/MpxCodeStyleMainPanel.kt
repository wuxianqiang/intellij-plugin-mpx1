// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.options

import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.application.options.codeStyle.arrangement.ArrangementSettingsPanel
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxCodeStyleMainPanel(currentSettings: CodeStyleSettings?, settings: CodeStyleSettings)
  : TabbedLanguageCodeStylePanel(MpxLanguage.INSTANCE, currentSettings, settings) {

  override fun initTabs(settings: CodeStyleSettings) {
    addIndentOptionsTab(settings)
    addSpacesTab(settings)
    addWrappingAndBracesTab(settings)
    addTab(ArrangementSettingsPanel(settings, MpxLanguage.INSTANCE))
  }

  override fun addSpacesTab(settings: CodeStyleSettings?) {
    addTab(object : MySpacesPanel(settings) {
      override fun doReformat(project: Project, psiFile: PsiFile): PsiFile {
        CodeStyleManager.getInstance(project).reformatText(psiFile, 0, psiFile.textLength)
        return psiFile
      }
    })
  }

  override fun addWrappingAndBracesTab(settings: CodeStyleSettings?) {
    addTab(object : MyWrappingAndBracesPanel(settings) {
      override fun doReformat(project: Project, psiFile: PsiFile): PsiFile {
        CodeStyleManager.getInstance(project).reformatText(psiFile, 0, psiFile.textLength)
        return psiFile
      }
    })
  }

  override fun addIndentOptionsTab(settings: CodeStyleSettings?) {
    val editor = LanguageCodeStyleSettingsProvider.forLanguage(defaultLanguage)?.indentOptionsEditor
    if (editor != null) {
      addTab(object : MyIndentOptionsWrapper(settings, editor) {
        override fun doReformat(project: Project, psiFile: PsiFile): PsiFile {
          CodeStyleManager.getInstance(project).reformatText(psiFile, 0, psiFile.textLength)
          return psiFile
        }
      })
    }
  }

}
