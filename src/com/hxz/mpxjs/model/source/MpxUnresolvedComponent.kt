// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.types.JSAnyType
import com.intellij.psi.PsiElement
import com.hxz.mpxjs.model.MpxComponent
import com.hxz.mpxjs.model.MpxEntitiesContainer
import com.hxz.mpxjs.model.getDefaultMpxComponentInstanceType

class MpxUnresolvedComponent(private val context: PsiElement) : MpxComponent {

  override val defaultName: String? = null
  override val source: PsiElement? = null
  override val parents: List<MpxEntitiesContainer> = emptyList()

  override val thisType: JSType
    get() = getDefaultMpxComponentInstanceType(context) ?: JSAnyType.get(context, false)

}
