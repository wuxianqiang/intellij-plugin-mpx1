// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.liveTemplate

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.lang.html.MpxFileType

private const val CONTEXT_TYPE = "MPX_TOP_LEVEL"

class MpxTopLevelLiveTemplateContextType : TemplateContextType(CONTEXT_TYPE, MpxBundle.message("mpx.live.template.context.top.level"),
                                                               MpxBaseLiveTemplateContextType::class.java) {
  override fun isInContext(file: PsiFile, offset: Int): Boolean {
    if (MpxFileType.INSTANCE == file.fileType) {
      val element = file.findElementAt(offset) ?: return true
      return PsiTreeUtil.getParentOfType(element, XmlTag::class.java) == null
    }
    return false
  }
}
