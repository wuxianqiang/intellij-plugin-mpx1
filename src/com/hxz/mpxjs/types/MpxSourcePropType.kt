// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.types

import com.intellij.lang.javascript.psi.*
import com.intellij.lang.javascript.psi.types.*
import com.intellij.util.ProcessingContext
import com.hxz.mpxjs.codeInsight.getJSTypeFromPropOptions

class MpxSourcePropType private constructor(typeSource: JSTypeSource, private val property: JSProperty)
  : JSSimpleTypeBaseImpl(typeSource), JSCodeBasedType {

  constructor(property: JSProperty) : this(JSTypeSourceFactory.createTypeSource(property, false), property)

  override fun copyWithNewSource(source: JSTypeSource): JSType =
    MpxSourcePropType(source, property)

  override fun isEquivalentToWithSameClass(type: JSType, context: ProcessingContext?, allowResolve: Boolean): Boolean =
    (type is MpxSourcePropType && type.property == property)

  override fun substituteImpl(context: JSTypeSubstitutionContext): JSType =
    getJSTypeFromPropOptions(property.value) ?: JSAnyType.get(source)

  override fun hashCodeImpl(): Int = property.name.hashCode()

  override fun buildTypeTextImpl(format: JSType.TypeTextFormat, builder: JSTypeTextBuilder) {
    if (format == JSType.TypeTextFormat.SIMPLE) {
      builder.append("#MpxSourcePropType: ").append(property.name ?: "")
      return
    }
    substitute().buildTypeText(format, builder)
  }

}