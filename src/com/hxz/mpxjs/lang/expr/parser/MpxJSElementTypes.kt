// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.javascript.types.JSExpressionElementType
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NonNls
import com.hxz.mpxjs.lang.expr.MpxJSLanguage
import com.hxz.mpxjs.lang.expr.psi.impl.*

object MpxJSElementTypes {

  val FILTER_ARGUMENTS_LIST: IElementType = MpxJSExpressionElementType(
    "FILTER_ARGUMENTS_LIST", ::MpxJSFilterArgumentsListImpl)

  val FILTER_REFERENCE_EXPRESSION: IElementType = MpxJSExpressionElementType(
    "FILTER_REFERENCE_EXPRESSION", ::MpxJSFilterReferenceExpressionImpl)

  val FILTER_LEFT_SIDE_ARGUMENT: IElementType = MpxJSExpressionElementType(
    "FILTER_LEFT_SIDE_ARGUMENT", ::MpxJSFilterLeftSideArgumentImpl)

  val FILTER_EXPRESSION: IElementType = MpxJSExpressionElementType(
    "FILTER_EXPRESSION", ::MpxJSFilterExpressionImpl)

  val V_FOR_EXPRESSION: IElementType = MpxJSExpressionElementType(
    "V_FOR_EXPRESSION", ::MpxJSVForExpressionImpl)

  val SLOT_PROPS_EXPRESSION: IElementType = MpxJSExpressionElementType(
    "SLOT_PROPS_EXPRESSION", ::MpxJSSlotPropsExpressionImpl)

  val EMBEDDED_EXPR_STATEMENT: IElementType = MpxJSElementType(
    "MPX:EMBEDDED_EXPR_STATEMENT", ::MpxJSEmbeddedExpressionImpl)


  private open class MpxJSElementType(@NonNls debugName: String, private val myClassConstructor: (MpxJSElementType) -> ASTNode)
    : IElementType(debugName, MpxJSLanguage.INSTANCE), ICompositeElementType {
    final override fun createCompositeNode(): ASTNode = myClassConstructor(this)
  }

  private class MpxJSExpressionElementType(@NonNls debugName: String,
                                           classConstructor: (MpxJSElementType) -> ASTNode)
    : MpxJSElementType(debugName, classConstructor), JSExpressionElementType


}

