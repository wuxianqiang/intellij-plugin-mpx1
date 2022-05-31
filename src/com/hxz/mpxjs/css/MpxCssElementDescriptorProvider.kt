// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.css

import com.intellij.psi.PsiElement
import com.intellij.psi.css.CssElementDescriptorProvider
import com.intellij.psi.css.CssSimpleSelector
import com.intellij.psi.css.descriptor.CssPseudoSelectorDescriptor
import com.intellij.psi.css.descriptor.CssPseudoSelectorDescriptorStub
import com.hxz.mpxjs.context.isMpxContext

class MpxCssElementDescriptorProvider : CssElementDescriptorProvider() {
  private val V_DEEP = "v-deep"
  private val PSEUDO_SELECTORS = listOf(CssPseudoSelectorDescriptorStub(V_DEEP, true))

  override fun isMyContext(context: PsiElement?): Boolean = context?.let { isMpxContext(it) } == true

  override fun findPseudoSelectorDescriptors(name: String, context: PsiElement?): Collection<CssPseudoSelectorDescriptor> {
    return if (context != null && V_DEEP == name) PSEUDO_SELECTORS else emptyList()
  }

  override fun getAllPseudoSelectorDescriptors(context: PsiElement?): Collection<CssPseudoSelectorDescriptor> = PSEUDO_SELECTORS

  override fun getDeclarationsForSimpleSelector(selector: CssSimpleSelector): Array<PsiElement> = PsiElement.EMPTY_ARRAY
}
