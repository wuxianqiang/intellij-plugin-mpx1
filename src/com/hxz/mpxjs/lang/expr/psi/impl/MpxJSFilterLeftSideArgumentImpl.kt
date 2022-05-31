// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.psi.impl

import com.intellij.lang.javascript.JSExtendedLanguagesTokenSetProvider
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.impl.JSElementImpl
import com.intellij.psi.tree.IElementType
import com.intellij.util.ArrayUtil
import com.hxz.mpxjs.lang.expr.parser.MpxJSElementTypes
import com.hxz.mpxjs.lang.expr.psi.MpxJSFilterLeftSideArgument

class MpxJSFilterLeftSideArgumentImpl(elementType: IElementType) : JSElementImpl(elementType), MpxJSFilterLeftSideArgument {

  internal val pipeLeftSideExpression: JSExpression?
    get() = findChildByType(JSExtendedLanguagesTokenSetProvider.EXPRESSIONS)
      ?.getPsi(JSExpression::class.java)

  val pipeRightSideArguments: MpxJSFilterArgumentsListImpl?
    get() = (parent as? MpxJSFilterExpressionImpl)
      ?.findChildByType(MpxJSElementTypes.FILTER_ARGUMENTS_LIST)
      ?.getPsi(MpxJSFilterArgumentsListImpl::class.java)

  private val pipeRightSideExpressions: Array<JSExpression>?
    get() = pipeRightSideArguments?.pipeRightSideExpressions

  override fun getArguments(): Array<JSExpression> {
    val leftExpr = pipeLeftSideExpression ?: return JSExpression.EMPTY_ARRAY
    val mainArgsList = pipeRightSideExpressions
    return if (mainArgsList == null)
      arrayOf(leftExpr)
    else
      ArrayUtil.prepend(leftExpr, mainArgsList)
  }

  override fun hasSpreadElement(): Boolean {
    return pipeRightSideArguments?.hasSpreadElement() == true
  }
}
