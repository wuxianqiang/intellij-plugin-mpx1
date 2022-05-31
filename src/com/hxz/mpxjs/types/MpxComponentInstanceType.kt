// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.types

import com.intellij.lang.javascript.psi.JSRecordType.PropertySignature
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.JSTypeTextBuilder
import com.intellij.lang.javascript.psi.JSTypeWithIncompleteSubstitution
import com.intellij.lang.javascript.psi.types.*
import com.intellij.util.ProcessingContext
import com.hxz.mpxjs.model.MpxInstanceOwner
import com.hxz.mpxjs.model.MpxNamedEntity
import java.util.*

class MpxComponentInstanceType(source: JSTypeSource,
                               private val instanceOwner: MpxInstanceOwner,
                               typeMembers: List<PropertySignature>)
  : JSSimpleTypeBaseImpl(source), JSCodeBasedType, JSTypeWithIncompleteSubstitution {

  private val typeMembers = typeMembers.toList()
  private val membersNames = typeMembers.map { it.memberName }

  override fun copyWithNewSource(source: JSTypeSource): JSType = MpxComponentInstanceType(source, instanceOwner, typeMembers)

  override fun acceptChildren(visitor: JSRecursiveTypeVisitor) {
    typeMembers.forEach { it.acceptChildren(visitor) }
  }

  override fun hashCodeImpl(): Int = Objects.hashCode(membersNames.toTypedArray()) * 31 + instanceOwner.hashCode()

  override fun isEquivalentToWithSameClass(type: JSType, context: ProcessingContext?, allowResolve: Boolean): Boolean {
    return (type is MpxComponentInstanceType
            && type.instanceOwner == instanceOwner
            && membersNames == membersNames)
  }

  override fun buildTypeTextImpl(format: JSType.TypeTextFormat, builder: JSTypeTextBuilder) {
    if (format == JSType.TypeTextFormat.SIMPLE) {
      builder.append("#MpxComponentInstanceType: ")
        .append(instanceOwner.javaClass.simpleName)
      if (instanceOwner is MpxNamedEntity) {
        builder.append("(").append(instanceOwner.defaultName).append(")")
      }
      builder.append(" [")
      membersNames.forEach { builder.append(it).append(",") }
      builder.append("]")
      return
    }
    substitute().buildTypeText(format, builder)
  }

  override fun substituteCompletely(): JSType = JSSimpleRecordTypeImpl(source, typeMembers)
}
