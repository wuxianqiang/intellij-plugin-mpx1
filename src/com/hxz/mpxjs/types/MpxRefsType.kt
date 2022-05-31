// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.types

import com.intellij.lang.javascript.psi.JSRecordType
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.JSTypeSubstitutionContext
import com.intellij.lang.javascript.psi.JSTypeTextBuilder
import com.intellij.lang.javascript.psi.types.*
import com.intellij.util.ProcessingContext
import com.hxz.mpxjs.codeInsight.REF_ATTRIBUTE_NAME
import com.hxz.mpxjs.index.findAttribute
import com.hxz.mpxjs.lang.html.psi.MpxRefAttribute
import com.hxz.mpxjs.model.MpxInstanceOwner
import com.hxz.mpxjs.model.MpxNamedEntity
import com.hxz.mpxjs.model.MpxRegularComponent
import com.hxz.mpxjs.model.getDefaultMpxComponentInstanceType

class MpxRefsType(source: JSTypeSource,
                  private val instanceOwner: MpxInstanceOwner) : JSSimpleTypeBaseImpl(source), JSCodeBasedType, MpxCompleteType {

  override fun copyWithNewSource(source: JSTypeSource): JSType = MpxRefsType(source, instanceOwner)

  override fun hashCodeImpl(): Int = instanceOwner.hashCode()

  override fun isEquivalentToWithSameClass(type: JSType, context: ProcessingContext?, allowResolve: Boolean): Boolean =
    type is MpxRefsType
    && type.instanceOwner == instanceOwner

  override fun buildTypeTextImpl(format: JSType.TypeTextFormat, builder: JSTypeTextBuilder) {
    if (format == JSType.TypeTextFormat.SIMPLE) {
      builder.append("#MpxRefsType: ")
        .append(instanceOwner.javaClass.simpleName)
      if (instanceOwner is MpxNamedEntity) {
        builder.append("(").append(instanceOwner.defaultName).append(")")
      }
      return
    }
    substitute().buildTypeText(format, builder)
  }

  override fun substituteImpl(context: JSTypeSubstitutionContext): JSType {
    val members: MutableMap<String, JSRecordType.TypeMember> = mutableMapOf()
    (instanceOwner as? MpxRegularComponent)?.template?.safeVisitTags { tag ->
      (findAttribute(tag, REF_ATTRIBUTE_NAME) as? MpxRefAttribute)
        ?.implicitElement
        ?.let {
          // For multiple elements with the same ref name, the last one is taken by Mpx engine
          members[it.name] = JSRecordTypeImpl.PropertySignatureImpl(it.name, it.jsType, false, true, it)
        }
    }
    getDefaultMpxComponentInstanceType(instanceOwner.source)
      ?.asRecordType()
      ?.findPropertySignature("\$refs")
      ?.jsType
      ?.asRecordType()
      ?.findIndexer(JSRecordType.IndexSignatureKind.STRING)
      ?.let { members[""] = it }
    return JSSimpleRecordTypeImpl(source, members.values.toList())
  }

}