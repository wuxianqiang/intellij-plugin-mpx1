// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.intentions.extractComponent

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BaseRefactoringAction
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.MpxjsIcons
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxExtractComponentAction : BaseRefactoringAction() {
  init {
    templatePresentation.text = MpxBundle.message("mpx.template.intention.extract.component")
    templatePresentation.description = MpxBundle.message("mpx.template.intention.extract.component.description")
    templatePresentation.icon = MpxjsIcons.Mpx
  }

  override fun isAvailableInEditorOnly(): Boolean = true

  override fun isEnabledOnElements(elements: Array<out PsiElement>): Boolean = true
  override fun isAvailableOnElementInEditorAndFile(element: PsiElement, editor: Editor, file: PsiFile, context: DataContext): Boolean {
    return MpxExtractComponentIntention.getContext(editor, element) != null
  }

  override fun isAvailableForLanguage(language: Language?): Boolean = MpxLanguage.INSTANCE == language

  override fun isAvailableForFile(file: PsiFile?): Boolean = MpxFileType.INSTANCE == file?.fileType

  override fun getHandler(dataContext: DataContext): RefactoringActionHandler? {
    return object : RefactoringActionHandler {
      override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        editor ?: return
        val element = PsiUtilBase.getElementAtCaret(editor) ?: return
        val context = MpxExtractComponentIntention.getContext(editor, element) ?: return
        MpxExtractComponentRefactoring(project, context, editor).perform()
      }

      override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // available only in editor
      }
    }
  }
}
