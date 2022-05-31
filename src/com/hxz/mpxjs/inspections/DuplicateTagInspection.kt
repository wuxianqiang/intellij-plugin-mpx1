// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.inspections

import com.intellij.codeInsight.daemon.impl.analysis.RemoveTagIntentionFix
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlDocument
import com.intellij.psi.xml.XmlTag
import com.intellij.xml.util.HtmlUtil
import com.intellij.xml.util.HtmlUtil.TEMPLATE_TAG_NAME
import com.intellij.xml.util.XmlTagUtil
import com.hxz.mpxjs.MpxBundle
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.lang.html.MpxLanguage

class DuplicateTagInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
    return object : XmlElementVisitor() {
      override fun visitXmlTag(tag: XmlTag?) {
        if (tag?.language != MpxLanguage.INSTANCE
            || tag.containingFile.originalFile.virtualFile.fileType != MpxFileType.INSTANCE) return
        if (TEMPLATE_TAG_NAME != tag.name && !HtmlUtil.isScriptTag(tag)) return
        val parent = tag.parent as? XmlDocument ?: return
        //检查重复标签的
        //if (PsiTreeUtil.getChildrenOfType(parent, XmlTag::class.java).any { it != tag && it.name == tag.name }) {
        //  val tagName = XmlTagUtil.getStartTagNameElement(tag)
        //  holder.registerProblem(tagName ?: tag,
        //                         MpxBundle.message("mpx.inspection.message.duplicate.tag", tag.name),
        //                         RemoveTagIntentionFix(tag.name, tag))
        //}
      }
    }
  }
}
