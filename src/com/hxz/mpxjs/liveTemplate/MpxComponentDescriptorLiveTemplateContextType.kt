// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.liveTemplate

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.lang.ecmascript6.psi.JSExportAssignment
import com.intellij.lang.javascript.JavaScriptCodeContextType
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.hxz.mpxjs.MpxBundle

private const val CONTEXT_TYPE = "MPX_COMPONENT_DESCRIPTOR"

class MpxComponentDescriptorLiveTemplateContextType : TemplateContextType(CONTEXT_TYPE,
                                                                          MpxBundle.message("mpx.live.template.context.component"),
                                                                          MpxBaseLiveTemplateContextType::class.java) {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    return MpxBaseLiveTemplateContextType.evaluateContext(
      file, offset,
      scriptContextEvaluator = { it is JSExportAssignment || PsiTreeUtil.getParentOfType(it, JSExportAssignment::class.java) != null },
      notMpxFileType = {
        JavaScriptCodeContextType.areJavaScriptTemplatesApplicable(it) &&
        PsiTreeUtil.getParentOfType(it, JSObjectLiteralExpression::class.java) != null
      })
  }
}
