// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.context

import com.intellij.lang.javascript.library.JSCDNLibManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.XmlRecursiveElementVisitor
import com.intellij.psi.impl.source.html.HtmlLikeFile
import com.intellij.psi.impl.source.xml.XmlTagImpl
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlTag
import com.intellij.javascript.web.context.WebFrameworkContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xml.util.HtmlUtil
import com.hxz.mpxjs.index.MPX_MODULE
import com.hxz.mpxjs.lang.html.MpxFileType
import com.hxz.mpxjs.lang.html.MpxLanguage

class MpxFileContext : WebFrameworkContext {

  override fun isEnabled(file: VirtualFile, project: Project): Boolean {
    return file.fileType == MpxFileType.INSTANCE
  }

  override fun isEnabled(file: PsiFile): Boolean {
    val vf = file.originalFile.virtualFile
    if (vf != null && isEnabled(vf, file.project)
        || (vf == null && file.language == MpxLanguage.INSTANCE)) {
      return true
    }
    //if (file is HtmlLikeFile) {
    //  return CachedValuesManager.getCachedValue(file) {
    //    CachedValueProvider.Result.create(hasMpxLibraryImport(file), file)
    //  }
    //}
    return false
  }
}

fun hasMpxLibraryImport(file: PsiFile): Boolean {
  var level = 0
  var result = false
  file.acceptChildren(object : XmlRecursiveElementVisitor() {
    override fun visitXmlTag(tag: XmlTag) {
      if (HtmlUtil.isScriptTag(tag) && hasMpxScriptLink(tag)) {
        result = true
      }
      if (++level <= 3) {
        // Do not process XIncludes to avoid recursion
        (tag as? XmlTagImpl)?.getSubTags(false)?.forEach { it.accept(this) }
        level--
      }
    }

    private fun hasMpxScriptLink(tag: XmlTag): Boolean {
      val link = tag.getAttribute(HtmlUtil.SRC_ATTRIBUTE_NAME)?.value
      if (link == null || !link.contains("mpx")) {
        return false
      }
      if (JSCDNLibManager.getLibraryForUrl(link)?.libraryName == MPX_MODULE) {
        return true
      }
      val fileName = VfsUtil.extractFileName(link)
      return fileName != null && fileName.startsWith("mpx.") && fileName.endsWith(".js")
    }
  })
  return result
}
