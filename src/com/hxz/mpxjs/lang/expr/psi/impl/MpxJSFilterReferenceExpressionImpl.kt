// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.psi.impl

import com.intellij.lang.javascript.psi.JSFunction
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.IncorrectOperationException
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterReferenceExpression

class MpxJSFilterReferenceExpressionImpl(elementType: IElementType)
  : JSReferenceExpressionImpl(elementType), MpxJSFilterReferenceExpression {

  override fun isReferenceTo(element: PsiElement): Boolean {
    return if (element is JSFunction) {
      element == resolve()
    }
    else false
  }

  @Throws(IncorrectOperationException::class)
  override fun handleElementRename(newElementName: String): PsiElement {
    return handleElementRenameInternal(newElementName)
  }
}
