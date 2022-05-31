// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.css.refs

import com.intellij.javascript.web.codeInsight.css.refs.CssClassInJSLiteralOrIdentifierReferenceProvider
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.hxz.mpxjs.codeInsight.attributes.MpxCustomAttributeValueProvider.Companion.isVBindClassAttribute
import com.hxz.mpxjs.lang.expr.MpxJSLanguage
import com.hxz.mpxjs.lang.expr.psi.MpxJSEmbeddedExpression

class MpxCssReferencesContributor : PsiReferenceContributor() {

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    CssClassInJSLiteralOrIdentifierReferenceProvider.register(registrar, MpxJSLanguage.INSTANCE,
                                                              MpxJSEmbeddedExpression::class.java, ::isVBindClassAttribute)
  }

}
