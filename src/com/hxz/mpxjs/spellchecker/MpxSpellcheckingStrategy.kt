// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.spellchecker

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.javascript.psi.JSEmbeddedContent
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.hxz.mpxjs.lang.expr.psi.MpxJSEmbeddedExpression

class MpxSpellcheckingStrategy : SpellcheckingStrategy() {
  override fun getTokenizer(element: PsiElement?): Tokenizer<*> {
    if (element is XmlAttributeValue) {
      val jsEmbeddedContent = PsiTreeUtil.getChildOfType(element, JSEmbeddedContent::class.java)
                              ?: PsiTreeUtil.getChildOfType(element, ASTWrapperPsiElement::class.java)
                                ?.let { PsiTreeUtil.getChildOfType(it, MpxJSEmbeddedExpression::class.java) }
      if (element.valueTextRange == jsEmbeddedContent?.textRange) return EMPTY_TOKENIZER
    }
    return super.getTokenizer(element)
  }
}
