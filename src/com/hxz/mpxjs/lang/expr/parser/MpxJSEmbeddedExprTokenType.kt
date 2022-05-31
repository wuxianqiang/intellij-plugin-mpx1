// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.expr.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.ILeafElementType
import com.hxz.mpxjs.codeInsight.attributes.MpxAttributeNameParser.MpxAttributeInfo
import com.hxz.mpxjs.lang.MpxEmbeddedContentTokenType
import com.hxz.mpxjs.lang.expr.MpxJSLanguage

class MpxJSEmbeddedExprTokenType private constructor(debugName: String,
                                                     private val attributeInfo: MpxAttributeInfo?,
                                                     private val project: Project?)
  : MpxEmbeddedContentTokenType(debugName, MpxJSLanguage.INSTANCE, false), ILeafElementType {

  companion object {
    fun createEmbeddedExpression(attributeInfo: MpxAttributeInfo,
                                 project: Project?): MpxJSEmbeddedExprTokenType {
      return MpxJSEmbeddedExprTokenType("MPX_JS:EMBEDDED_EXPR", attributeInfo, project)
    }

    fun createInterpolationExpression(project: Project?): MpxJSEmbeddedExprTokenType {
      return MpxJSEmbeddedExprTokenType("MPX_JS:INTERPOLATION_EXPR", null, project)
    }
  }

  override fun createLexer(): Lexer {
    return MpxJSParserDefinition.createLexer(project)
  }

  override fun parse(builder: PsiBuilder) {
    MpxJSParser.parseEmbeddedExpression(builder, this, attributeInfo)
  }

  override fun hashCode(): Int {
    var result = attributeInfo.hashCode()
    result = 31 * result + project.hashCode()
    return result
  }

  override fun createLeafNode(leafText: CharSequence): ASTNode = LeafPsiElement(this, leafText)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as MpxJSEmbeddedExprTokenType
    return attributeInfo == other.attributeInfo
           && project == other.project
  }
}
