// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.javascript.web.codeInsight.css.CssInBindingExpressionCompletionProvider
import com.intellij.lang.Language
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.patterns.JSPatterns
import com.intellij.lang.javascript.psi.JSThisExpression
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.ProcessingContext
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeValueCompletionProvider
import com.hxz.mpxjs.lang.expr.MpxJSLanguage

class MpxCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, psiElement(XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN),
           MpxAttributeValueCompletionProvider())
    extend(CompletionType.BASIC, psiElement().with(language(MpxJSLanguage.INSTANCE)),
           MpxJSCompletionProvider())
    extend(CompletionType.BASIC, psiElement().with(language(MpxJSLanguage.INSTANCE)),
           CssInBindingExpressionCompletionProvider())
    extend(CompletionType.BASIC,
           psiElement(JSTokenTypes.IDENTIFIER)
             .withParent(JSPatterns.jsReferenceExpression().withFirstChild(psiElement(JSThisExpression::class.java))),
           MpxThisInstanceCompletionProvider())
  }

  // TODO merge with Angular
  private fun <T : PsiElement> language(language: Language): PatternCondition<T> {
    return object : PatternCondition<T>("language(" + language.id + ")") {
      override fun accepts(t: T, context: ProcessingContext): Boolean {
        return language.`is`(PsiUtilCore.findLanguageFromElement(t))
      }
    }
  }
}

