// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model

import com.intellij.lang.javascript.psi.JSType
import com.hxz.mpxjs.codeInsight.documentation.MpxDocumentedItem

interface MpxDirective : MpxNamedEntity, MpxScopeElement, MpxDocumentedItem {
  val acceptsNoValue: Boolean get() = true
  val acceptsValue: Boolean get() = true
  val jsType: JSType? get() = null
  val modifiers: List<MpxDirectiveModifier> get() = emptyList()
  val argument: MpxDirectiveArgument? get() = null
}
