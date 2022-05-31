// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider.Result.create
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.util.castSafelyTo
import com.hxz.mpxjs.codeInsight.getTextIfLiteral
import com.hxz.mpxjs.index.MpxIndexData
import com.hxz.mpxjs.model.*

class MpxSourceComponent(sourceElement: JSImplicitElement,
                         descriptor: MpxSourceEntityDescriptor,
                         private val indexData: MpxIndexData?)
  : MpxSourceContainer(sourceElement, descriptor), MpxRegularComponent {

  override val defaultName: String?
    get() = indexData?.originalName
            ?: getTextIfLiteral(descriptor.initializer?.castSafelyTo<JSObjectLiteralExpression>()
                                  ?.findProperty(NAME_PROP)?.value)

  override val slots: List<MpxSlot>
    get() {
      val template = template ?: return emptyList()
      return CachedValuesManager.getCachedValue(template.source) {
        create(buildSlotsList(template), template.source)
      }
    }

  companion object {

    private fun buildSlotsList(template: MpxTemplate<*>): List<MpxSlot> {
      val result = mutableListOf<MpxSlot>()
      template.safeVisitTags { tag ->
        if (tag.name == SLOT_TAG_NAME) {
          val name = PsiTreeUtil.getStubChildrenOfTypeAsList(tag, XmlAttribute::class.java)
                       .find { it.name == SLOT_NAME_ATTRIBUTE }
                       ?.value
                       ?.takeIf { it.isNotBlank() }
                     ?: DEFAULT_SLOT_NAME
          result.add(MpxSourceSlot(name, tag))
        }
      }
      return result
    }
  }

  private class MpxSourceSlot(override val name: String,
                              override val source: PsiElement) : MpxSlot
}
