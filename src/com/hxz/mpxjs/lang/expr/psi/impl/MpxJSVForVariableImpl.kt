// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.impl.JSVariableImpl
import com.intellij.lang.javascript.psi.stubs.JSVariableStubBase
import com.intellij.lang.javascript.psi.types.JSPsiBasedTypeOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.hxz.mpxjs.lang.expr.psi.MpxJSVForExpression
import com.hxz.mpxjs.lang.expr.psi.MpxJSVForVariable

class MpxJSVForVariableImpl(node: ASTNode) : JSVariableImpl<JSVariableStubBase<JSVariable>, JSVariable>(node), MpxJSVForVariable {

  override fun hasBlockScope(): Boolean = true

  override fun calculateType(): JSType? {
    return PsiTreeUtil.getParentOfType(this, MpxJSVForExpression::class.java)
      ?.getVarStatement()
      ?.declarations
      ?.takeIf { it.indexOf(this) in 0..2 }
      ?.let { JSPsiBasedTypeOfType(this, false) }
  }

  override fun getDeclarationScope(): PsiElement? =
    PsiTreeUtil.getContextOfType(this, XmlTag::class.java, PsiFile::class.java)

}
