// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.refactoring

import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.stubs.impl.JSImplicitElementImpl
import com.intellij.lang.javascript.refactoring.JSDefaultRenameProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.html.HtmlTag
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.usageView.UsageInfo
import com.hxz.mpxjs.codeInsight.fromAsset
import com.hxz.mpxjs.codeInsight.toAsset
import com.hxz.mpxjs.index.MpxComponentsIndex

class MpxJSComponentRenameProcessor : JSDefaultRenameProcessor() {


  override fun renameElement(element: PsiElement, newName: String, usages: Array<out UsageInfo>, listener: RefactoringElementListener?) {
    for (usage in usages) {
      val usageElement = usage.element
      if (usageElement is HtmlTag && toAsset(usageElement.name) != usageElement.name) {
        RenameUtil.rename(usage, fromAsset(newName))
      }
      else {
        RenameUtil.rename(usage, newName)
      }
    }
  }

  override fun findReferences(element: PsiElement,
                              searchScope: SearchScope,
                              searchInCommentsAndStrings: Boolean): MutableCollection<PsiReference> {
    return ReferencesSearch.search(element, searchScope).findAll()
  }

  override fun canProcessElement(element: PsiElement): Boolean {
    return MpxRefactoringUtils.isComponentName(element)
  }

  override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? {
    return element as? JSImplicitElement ?: JSImplicitElementImpl.Builder(element.text, element)
      .setUserString(MpxComponentsIndex.JS_KEY)
      .toImplicitElement()
  }
}
