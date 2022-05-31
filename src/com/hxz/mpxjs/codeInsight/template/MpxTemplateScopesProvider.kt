// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.codeInsight.template

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

abstract class MpxTemplateScopesProvider {

  /**
   * If Angular expression is injected the @{code hostElement} is not null.
   */
  abstract fun getScopes(element: PsiElement,
                         hostElement: PsiElement?): List<MpxTemplateScope>

  companion object {
    internal val EP_NAME = ExtensionPointName.create<MpxTemplateScopesProvider>("com.intellij.mpxjs.templateScopesProvider")
  }
}
