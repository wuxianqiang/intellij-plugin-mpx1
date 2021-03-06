// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler
import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplate.ATTRIBUTE_NAME
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.hxz.mpxjs.CreateMpxSingleFileComponentAction.Companion.MPX_TEMPLATE_NAME
import com.hxz.mpxjs.lang.html.MpxFileType

class MpxCreateFromTemplateHandler : DefaultCreateFromTemplateHandler() {
  override fun handlesTemplate(template: FileTemplate): Boolean {
    val fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(template.extension)
    return MpxFileType.INSTANCE == fileType && MPX_TEMPLATE_NAME == template.name
  }

  override fun isNameRequired(): Boolean = true

  override fun prepareProperties(props: MutableMap<String, Any>) {
    val name = props[ATTRIBUTE_NAME] as? String ?: return
    props["COMPONENT_NAME"] = name
  }
}
