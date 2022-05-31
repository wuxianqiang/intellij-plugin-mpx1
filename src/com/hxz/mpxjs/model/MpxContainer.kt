// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

import com.intellij.lang.javascript.psi.JSType
import com.intellij.psi.PsiElement
import com.hxz.mpxjs.codeInsight.documentation.MpxDocumentedItem

interface MpxContainer : MpxEntitiesContainer {
  val data: List<MpxDataProperty>
  val computed: List<MpxComputedProperty>
  val methods: List<MpxMethod>
  val props: List<MpxInputProperty>
  val emits: List<MpxEmitCall>
  val slots: List<MpxSlot>

  val template: MpxTemplate<*>? get() = null
  val element: String? get() = null
  val extends: List<MpxContainer>
  val delimiters: Pair<String, String>? get() = null
  val model: MpxModelDirectiveProperties
}

class MpxModelDirectiveProperties(
  val prop: String = DEFAULT_PROP,
  val event: String = DEFAULT_EVENT
) {
  companion object {
    const val DEFAULT_PROP = "value"
    const val DEFAULT_EVENT = "input"
  }
}

interface MpxNamedSymbol : MpxDocumentedItem {
  val name: String
  val source: PsiElement? get() = null
}

interface MpxSlot : MpxNamedSymbol {
  val scope: JSType? get() = null
  val pattern: Regex? get() = null
}

interface MpxEmitCall : MpxNamedSymbol {
  val eventJSType: JSType? get() = null
}

interface MpxProperty : MpxNamedSymbol {
  val jsType: JSType? get() = null
}

interface MpxInputProperty : MpxProperty {
  val required: Boolean
  val defaultValue: String? get() = null
}

interface MpxDataProperty : MpxProperty

interface MpxComputedProperty : MpxProperty

interface MpxMethod : MpxProperty
