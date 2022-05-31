// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight

import com.intellij.lang.javascript.psi.JSInheritedLanguagesConfigurableProvider
import com.intellij.lang.javascript.psi.JSInitializerOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SyntaxTraverser
import com.hxz.mpxjs.lang.expr.MpxJSLanguage
import com.hxz.mpxjs.lang.html.MpxFileType

class MpxJSLanguageConfigurableProvider : JSInheritedLanguagesConfigurableProvider() {
  override fun isNeedToBeTerminated(element: PsiElement): Boolean = false

  override fun createParameterOrVariableItem(destruct: String, parent: PsiElement): PsiElement? {
    if (parent.language !is MpxJSLanguage) return null
    val file = PsiFileFactory.getInstance(parent.project).createFileFromText("q.mpx", MpxFileType.INSTANCE,
                                                                             "<li v-for=\"$destruct in schedules\">")
    return SyntaxTraverser.psiTraverser(file).filter(JSInitializerOwner::class.java).first()
  }
}
