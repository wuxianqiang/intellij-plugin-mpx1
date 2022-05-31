// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.hxz.mpxjs.lang.html.psi.formatter

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.lang.xml.XmlFormattingModel
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import com.intellij.psi.formatter.xml.HtmlPolicy
import com.intellij.psi.xml.XmlTag
import com.hxz.mpxjs.lang.html.MpxFileType

class MpxFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(formattingContext: FormattingContext): FormattingModel {
    val psiFile = formattingContext.containingFile
    val mpxFile = psiFile.originalFile.virtualFile?.let { it.fileType === MpxFileType.INSTANCE } ?: true
    val documentModel = FormattingDocumentModelImpl.createOn(psiFile)
    val element = formattingContext.psiElement
    val settings = formattingContext.codeStyleSettings
    return if (element is XmlTag) {
      XmlFormattingModel(
        psiFile,
        MpxHtmlTagBlock(element.node, null, null, HtmlPolicy(settings, documentModel),
                        null, false),
        documentModel)
    }
    else {
      XmlFormattingModel(
        psiFile,
        if (mpxFile)
          MpxBlock(psiFile.node, null, null, MpxRootFormattingPolicy(settings, documentModel),
                   null, null, false)
        else
          MpxHtmlBlock(psiFile.node, null, null, HtmlPolicy(settings, documentModel),
                       null, null, false),
        documentModel)
    }
  }
}