// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.liveTemplate

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile
import com.hxz.mpxjs.MpxBundle

private const val CONTEXT_TYPE = "MPX_INSIDE_TAG"

class MpxInsideTagLiveTemplateContextType : TemplateContextType(CONTEXT_TYPE,
                                                                MpxBundle.message("mpx.live.template.context.template.tag.element"),
                                                                MpxBaseLiveTemplateContextType::class.java) {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    return MpxBaseLiveTemplateContextType.evaluateContext(file, offset, forAttributeInsert = true)
  }
}
