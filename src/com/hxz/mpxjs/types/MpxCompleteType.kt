// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.types

import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.JSTypeSubstitutionContext
import com.intellij.lang.javascript.psi.JSTypeTextBuilder
import com.intellij.lang.javascript.psi.types.JSSimpleTypeBaseImpl
import com.intellij.lang.javascript.psi.types.JSTypeSource
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

interface MpxCompleteType : JSType

fun createStrictTypeSource(element: PsiElement?) =
  JSTypeSource(element, JSTypeSource.SourceLanguage.TS, true)

fun JSType.asCompleteType(): MpxCompleteType =
  this as? MpxCompleteType ?: MpxCompleteTypeImpl(this)

private class MpxCompleteTypeImpl(private val baseType: JSType, source: JSTypeSource) : JSSimpleTypeBaseImpl(source), MpxCompleteType {

  constructor(baseType: JSType) : this(baseType, baseType.source)

  override fun copyWithNewSource(source: JSTypeSource): JSType = MpxCompleteTypeImpl(baseType, source)

  override fun hashCodeImpl(): Int = baseType.hashCode()

  override fun isEquivalentToWithSameClass(type: JSType, context: ProcessingContext?, allowResolve: Boolean): Boolean =
    type is MpxCompleteTypeImpl
    && type.baseType.isEquivalentTo(baseType, context, allowResolve)

  override fun buildTypeTextImpl(format: JSType.TypeTextFormat, builder: JSTypeTextBuilder) {
    if (format == JSType.TypeTextFormat.SIMPLE) {
      builder.append("#MpxCompleteTypeImpl(")
      baseType.buildTypeText(format, builder)
      builder.append(")")
      return
    }
    substitute().buildTypeText(format, builder)
  }

  override fun substituteImpl(context: JSTypeSubstitutionContext): JSType = baseType

}