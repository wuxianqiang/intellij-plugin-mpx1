// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.model.source

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.hxz.mpxjs.model.MpxEntitiesContainer
import com.hxz.mpxjs.model.MpxFilter
import com.hxz.mpxjs.model.MpxGlobalImpl

class MpxSourceFilter(override val defaultName: String,
                      private val originalSource: PsiElement) : MpxFilter {

  override val parents: List<MpxEntitiesContainer> get() = MpxGlobalImpl.getParents(this)

  override val source: PsiElement? get() {
    return (originalSource as? PsiReference)?.resolve() ?: originalSource
  }

}
