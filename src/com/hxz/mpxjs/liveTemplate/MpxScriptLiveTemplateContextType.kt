// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.liveTemplate

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.lang.javascript.JSStatementContextType
import com.intellij.lang.javascript.psi.JSEmbeddedContent
import com.intellij.lang.javascript.psi.JSExpressionStatement
import com.intellij.psi.PsiFile
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.liveTemplate.MpxBaseLiveTemplateContextType.Companion.evaluateContext
import com.hxz.mpxjs.liveTemplate.MpxBaseLiveTemplateContextType.Companion.isTagEnd

private const val CONTEXT_TYPE = "MPX_SCRIPT"

class MpxScriptLiveTemplateContextType : TemplateContextType(CONTEXT_TYPE, MpxBundle.message("mpx.live.template.context.script.tag"),
                                                             MpxBaseLiveTemplateContextType::class.java) {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    return evaluateContext(file, offset,
                           scriptContextEvaluator = { isTagEnd(it) || it.parent is JSEmbeddedContent && it is JSExpressionStatement },
                           notMpxFileType = { JSStatementContextType.isInContext(it) })
  }
}
