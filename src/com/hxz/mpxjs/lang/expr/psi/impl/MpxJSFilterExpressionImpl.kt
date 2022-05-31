// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.psi.impl

import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.impl.JSExpressionImpl
import com.intellij.lang.javascript.psi.stubs.JSElementIndexingData
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.tree.IElementType
import com.hxz.mpxjs.lang.expr.parser.MpxJSElementTypes
import com.hxz.mpxjs.lang.expr.psi.MpxJSElementVisitor
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterArgumentsList
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterExpression

class MpxJSFilterExpressionImpl(elementType: IElementType)
  : JSExpressionImpl(elementType), MpxJSFilterExpression, JSCallLikeExpressionCommon {

  private val leftSideArgument: MpxJSFilterLeftSideArgumentImpl
    get() {
      return findChildByType(MpxJSElementTypes.FILTER_LEFT_SIDE_ARGUMENT)
        ?.getPsi(MpxJSFilterLeftSideArgumentImpl::class.java)!!
    }

  private val nameReference: JSReferenceExpression?
    get() = findPsiChildByType(MpxJSElementTypes.FILTER_REFERENCE_EXPRESSION) as JSReferenceExpression?

  override fun accept(visitor: PsiElementVisitor) {
    when (visitor) {
      is MpxJSElementVisitor -> visitor.visitMpxJSFilterExpression(this)
      is JSElementVisitor -> visitor.visitJSCallExpression(this)
      else -> super.accept(visitor)
    }
  }

  override fun getIndexingData(): JSElementIndexingData? = null

  override fun getName(): String? = nameReference?.referenceName

  override fun getMethodExpression(): JSExpression? = nameReference

  override fun getStubSafeMethodExpression(): JSExpression? = null

  override fun getArgumentList(): JSArgumentList = leftSideArgument

  override val filterArgumentsList: MpxJSFilterArgumentsList? get() = leftSideArgument.pipeRightSideArguments

  override fun isRequireCall(): Boolean = false

  override fun isDefineCall(): Boolean = false

  override fun isElvis(): Boolean = false
}
